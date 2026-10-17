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
import { ApplicationDetail, ApplicationStatus } from '../../core/models/api.models';
import { JobStore } from '../../core/state/job.store';
import { ToastService } from '../../core/state/toast.service';
import { errorMessage } from '../../core/util/http-error';

export type SaveState = 'idle' | 'saving' | 'saved' | 'error';

/** Zustand der Bewerber-Detailseite (AI Analytics View). */
@Injectable()
export class ApplicationDetailStore {
  private readonly api = inject(ApplicationApiService);
  private readonly jobStore = inject(JobStore);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly notes$ = new Subject<string>();
  private resumeObjectUrl: string | null = null;

  readonly detail = signal<ApplicationDetail | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly analyzing = signal(false);
  readonly analyzeError = signal<string | null>(null);
  readonly generating = signal(false);
  readonly notesState = signal<SaveState>('idle');
  readonly resumeUrl = signal<string | null>(null);
  readonly resumeLoading = signal(false);
  readonly resumeError = signal<string | null>(null);

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
    this.destroyRef.onDestroy(() => this.revokeResume());
  }

  load(id: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.revokeResume();
    this.api
      .get(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (detail) => {
          this.detail.set(detail);
          this.jobStore.select(detail.job.id);
          this.loading.set(false);
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

  /** Lädt das Original-PDF als Blob-URL für die PDF-Vorschau (einmal pro Bewerbung). */
  loadResume(): void {
    const current = this.detail();
    if (!current?.resumeFileName || this.resumeUrl() || this.resumeLoading()) {
      return;
    }
    this.resumeLoading.set(true);
    this.resumeError.set(null);
    this.api
      .resume(current.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (blob) => {
          this.resumeObjectUrl = URL.createObjectURL(new Blob([blob], { type: 'application/pdf' }));
          this.resumeUrl.set(this.resumeObjectUrl);
          this.resumeLoading.set(false);
        },
        error: (err: unknown) => {
          this.resumeError.set(errorMessage(err, 'Das PDF konnte nicht geladen werden.'));
          this.resumeLoading.set(false);
        },
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

  private revokeResume(): void {
    if (this.resumeObjectUrl) {
      URL.revokeObjectURL(this.resumeObjectUrl);
      this.resumeObjectUrl = null;
    }
    this.resumeUrl.set(null);
  }
}
