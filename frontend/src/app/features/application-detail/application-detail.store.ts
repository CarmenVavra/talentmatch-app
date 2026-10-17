import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  Subject,
  catchError,
  debounceTime,
  distinctUntilChanged,
  map,
  of,
  switchMap,
  tap,
} from 'rxjs';

import { ApplicationApiService } from '../../core/api/application-api.service';
import { ApplicationDetail, ApplicationStatus, DocumentKind } from '../../core/models/api.models';
import { JobStore } from '../../core/state/job.store';
import { ToastService } from '../../core/state/toast.service';
import { errorMessage } from '../../core/util/http-error';

export type SaveState = 'idle' | 'saving' | 'saved' | 'error';

type ByDocument<T> = Partial<Record<DocumentKind, T>>;

/** Dateiname einer eingereichten Unterlage oder null, wenn sie fehlt. */
export function documentFileName(detail: ApplicationDetail, kind: DocumentKind): string | null {
  switch (kind) {
    case 'resume':
      return detail.resumeFileName;
    case 'skill-profile':
      return detail.skillProfileFileName;
    case 'cover-letter':
      return detail.coverLetterFileName;
  }
}

/** Zustand der Bewerber-Detailseite (AI Analytics View). */
@Injectable()
export class ApplicationDetailStore {
  private readonly api = inject(ApplicationApiService);
  private readonly jobStore = inject(JobStore);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly notes$ = new Subject<string>();
  private readonly objectUrls = new Map<DocumentKind, string>();

  readonly detail = signal<ApplicationDetail | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly analyzing = signal(false);
  readonly analyzeError = signal<string | null>(null);
  readonly generating = signal(false);
  readonly notesState = signal<SaveState>('idle');
  /** Blob-URLs der geladenen Original-PDFs je Unterlage. */
  readonly documentUrls = signal<ByDocument<string>>({});
  readonly documentLoading = signal<ByDocument<boolean>>({});
  readonly documentErrors = signal<ByDocument<string>>({});

  readonly analysis = computed(() => this.detail()?.analysis ?? null);
  readonly questions = computed(() => this.analysis()?.questions ?? []);
  readonly discussedCount = computed(() => this.questions().filter((q) => q.discussed).length);

  constructor() {
    this.notes$
      .pipe(
        debounceTime(700),
        distinctUntilChanged(),
        tap(() => this.notesState.set('saving')),
        switchMap((notes) =>
          this.api.updateNotes(this.requireId(), notes).pipe(
            map((): SaveState => 'saved'),
            catchError(() => of<SaveState>('error')),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((state) => this.notesState.set(state));
    this.destroyRef.onDestroy(() => this.revokeDocuments());
  }

  load(id: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.revokeDocuments();
    this.api
      .get(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (detail) => {
          this.detail.set(detail);
          this.jobStore.select(detail.job.id);
          this.loading.set(false);
          if (detail.unread) {
            this.markSeen(detail.id);
          }
        },
        error: (err: unknown) => {
          this.error.set(errorMessage(err, 'Die Bewerbung konnte nicht geladen werden.'));
          this.loading.set(false);
        },
      });
  }

  setStatus(status: ApplicationStatus): void {
    const current = this.detail();
    if (!current || current.status === status) {
      return;
    }
    this.detail.set({ ...current, status });
    this.api
      .updateStatus(current.id, status)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        error: (err: unknown) => {
          this.detail.update((d) => (d ? { ...d, status: current.status } : d));
          this.toast.error(errorMessage(err, 'Der Status konnte nicht geändert werden.'));
        },
      });
  }

  toggleQuestion(index: number): void {
    const current = this.detail();
    const question = current?.analysis?.questions.find((q) => q.index === index);
    if (!current?.analysis || !question) {
      return;
    }
    const discussed = !question.discussed;
    this.patchQuestion(index, discussed);
    this.api
      .markQuestion(current.id, index, discussed)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        error: (err: unknown) => {
          this.patchQuestion(index, !discussed);
          this.toast.error(errorMessage(err, 'Die Frage konnte nicht gespeichert werden.'));
        },
      });
  }

  updateNotes(notes: string): void {
    this.notes$.next(notes);
  }

  analyze(): void {
    const current = this.detail();
    if (!current || this.analyzing()) {
      return;
    }
    this.analyzing.set(true);
    this.analyzeError.set(null);
    this.api
      .analyze(current.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (detail) => {
          this.detail.set(detail);
          this.analyzing.set(false);
        },
        error: (err: unknown) => {
          this.analyzing.set(false);
          this.analyzeError.set(
            `Analyse fehlgeschlagen – erneut versuchen. ${errorMessage(err, '')}`.trim(),
          );
        },
      });
  }

  generateMoreQuestions(): void {
    const current = this.detail();
    if (!current || this.generating()) {
      return;
    }
    this.generating.set(true);
    this.api
      .generateQuestions(current.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (detail) => {
          this.detail.update((d) => (d ? { ...d, analysis: detail.analysis } : detail));
          this.generating.set(false);
        },
        error: (err: unknown) => {
          this.generating.set(false);
          this.toast.error(errorMessage(err, 'Es konnten keine weiteren Fragen generiert werden.'));
        },
      });
  }

  /** Lädt das Original-PDF einer Unterlage als Blob-URL für die PDF-Vorschau (einmal pro Bewerbung). */
  loadDocument(kind: DocumentKind): void {
    const current = this.detail();
    if (
      !current ||
      !documentFileName(current, kind) ||
      this.documentUrls()[kind] ||
      this.documentLoading()[kind]
    ) {
      return;
    }
    this.documentLoading.update((loading) => ({ ...loading, [kind]: true }));
    this.documentErrors.update((errors) => ({ ...errors, [kind]: undefined }));
    this.api
      .document(current.id, kind)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (blob) => {
          const url = URL.createObjectURL(new Blob([blob], { type: 'application/pdf' }));
          this.objectUrls.set(kind, url);
          this.documentUrls.update((urls) => ({ ...urls, [kind]: url }));
          this.documentLoading.update((loading) => ({ ...loading, [kind]: false }));
        },
        error: (err: unknown) => {
          const message = errorMessage(err, 'Das PDF konnte nicht geladen werden.');
          this.documentErrors.update((errors) => ({ ...errors, [kind]: message }));
          this.documentLoading.update((loading) => ({ ...loading, [kind]: false }));
        },
      });
  }

  /** Entfernt die „Neu“-Markierung; schlägt das fehl, bleibt sie einfach bis zum nächsten Öffnen. */
  private markSeen(id: number): void {
    this.api
      .markSeen(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.detail.update((d) => (d?.id === id ? { ...d, unread: false } : d)),
        error: () => undefined,
      });
  }

  private patchQuestion(index: number, discussed: boolean): void {
    this.detail.update((d) =>
      d?.analysis
        ? {
            ...d,
            analysis: {
              ...d.analysis,
              questions: d.analysis.questions.map((q) =>
                q.index === index ? { ...q, discussed } : q,
              ),
            },
          }
        : d,
    );
  }

  private requireId(): number {
    const id = this.detail()?.id;
    if (id === undefined) {
      throw new Error('Keine Bewerbung geladen');
    }
    return id;
  }

  private revokeDocuments(): void {
    this.objectUrls.forEach((url) => URL.revokeObjectURL(url));
    this.objectUrls.clear();
    this.documentUrls.set({});
    this.documentLoading.set({});
    this.documentErrors.set({});
  }
}
