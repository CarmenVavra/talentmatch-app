import { HttpEventType } from '@angular/common/http';
import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, delay, filter, map, switchMap, tap } from 'rxjs';

import { ApplicationApiService } from '../../core/api/application-api.service';
import {
  ApplicationDetail,
  ApplicationDocuments,
  ApplicationSummary,
  DocumentType,
  GroupMatch,
  InspectedFile,
} from '../../core/models/api.models';
import { toSummary } from '../../core/util/application';
import { documentTypeLabel } from '../../core/util/document-type';
import { validateFile } from '../../core/util/file';
import { errorMessage } from '../../core/util/http-error';
import { PipelineStore } from './pipeline.store';

export type UploadStage = 'uploading' | 'uploaded' | 'extracted' | 'analyzing' | 'done' | 'error';

/** Eine hochzuladende Bewerbung (eine Person) mit Fortschritt. */
export interface UploadItem {
  id: number;
  /** Anzeige: Name der Person bzw. Dateiname. */
  label: string;
  fileNames: string[];
  sizeBytes: number;
  /** Fehlt bei ungültigen Dateien, die gar nicht hochgeladen werden. */
  documents: ApplicationDocuments | null;
  stage: UploadStage;
  /** Fortschritt in Prozent (Breite des Balkens). */
  progress: number;
  score: number | null;
  error: string | null;
  /** Ungültige Dateien (Typ, Größe) lassen sich nicht erneut versuchen. */
  retryable: boolean;
  /** Gesetzt, sobald die Bewerbung angelegt wurde – Fehler danach betreffen nur die Analyse. */
  applicationId: number | null;
}

/** Datei in der Vorschau, vor dem Hochladen. */
export interface DraftFile {
  key: number;
  file: File;
  type: DocumentType;
  info: InspectedFile;
}

/** Vorgeschlagene Person: Ihre Dateien ergeben eine Bewerbung. */
export interface DraftGroup {
  id: number;
  files: DraftFile[];
  match: GroupMatch;
  needsReview: boolean;
}

const STAGE_PROGRESS: Record<Exclude<UploadStage, 'uploading' | 'error'>, number> = {
  uploaded: 15,
  extracted: 45,
  analyzing: 75,
  done: 100,
};

/** Was an einer Gruppe noch nicht stimmt (sonst null) – jede Bewerbung braucht genau einen Lebenslauf. */
export function groupProblem(group: DraftGroup): string | null {
  const counts = new Map<DocumentType, number>();
  for (const file of group.files) {
    counts.set(file.type, (counts.get(file.type) ?? 0) + 1);
  }
  if (!counts.get('RESUME')) {
    return 'Lebenslauf fehlt – bitte eine Datei als Lebenslauf markieren.';
  }
  const doubled = [...counts.entries()].find(([, count]) => count > 1);
  return doubled ? `${documentTypeLabel(doubled[0])} ist mehrfach vorhanden.` : null;
}

/** Name einer Person für die Anzeige: aus dem Lebenslauf, sonst aus der ersten Datei. */
export function groupLabel(group: DraftGroup): string {
  const source = group.files.find((f) => f.type === 'RESUME') ?? group.files[0];
  const { firstName, lastName } = source?.info ?? { firstName: null, lastName: null };
  return firstName && lastName && lastName !== 'Bewerber:in'
    ? `${firstName} ${lastName}`
    : (source?.file.name ?? 'Neue Bewerbung');
}

/**
 * Upload-Dialog: Dateien prüfen und nach Personen gruppieren (Vorschau) → je Person eine Bewerbung hochladen
 * → Text extrahiert (Tika) → KI-Analyse → Karte in „KI-analysiert“.
 */
@Injectable()
export class UploadStore {
  private readonly api = inject(ApplicationApiService);
  private readonly pipeline = inject(PipelineStore);
  private readonly destroyRef = inject(DestroyRef);
  private nextId = 1;

  readonly uploads = signal<UploadItem[]>([]);
  readonly hasUploads = computed(() => this.uploads().length > 0);

  /** Vorschau: geprüfte, noch nicht hochgeladene Dateien, gruppiert nach Person. */
  readonly groups = signal<DraftGroup[]>([]);
  readonly inspecting = signal(false);
  readonly inspectError = signal<string | null>(null);
  readonly hasDraft = computed(() => this.groups().length > 0);
  readonly draftFileCount = computed(() =>
    this.groups().reduce((sum, group) => sum + group.files.length, 0),
  );
  readonly canSubmit = computed(
    () => this.hasDraft() && !this.inspecting() && this.groups().every((g) => !groupProblem(g)),
  );

  /** Neue Dateien: ungültige sofort als Fehler, gültige zusammen mit der Vorschau neu prüfen und gruppieren. */
  addFiles(jobId: number, files: readonly File[]): void {
    const valid: File[] = [];
    for (const file of files) {
      const validationError = validateFile(file);
      if (validationError) {
        this.addError(file, validationError);
      } else {
        valid.push(file);
      }
    }
    if (valid.length === 0) {
      return;
    }
    const all = [...this.groups().flatMap((g) => g.files.map((f) => f.file)), ...valid];
    this.inspecting.set(true);
    this.inspectError.set(null);
    this.api
      .inspect(jobId, all)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (result) => {
          for (const info of result.files.filter((f) => f.error)) {
            this.addError(all[info.index], info.error ?? 'Datei nicht verwendbar');
          }
          this.groups.set(
            result.groups.map((group) => ({
              id: this.nextId++,
              match: group.match,
              needsReview: group.needsReview,
              files: group.fileIndexes.map((index) => ({
                key: this.nextId++,
                file: all[index],
                type: result.files[index].type ?? 'RESUME',
                info: result.files[index],
              })),
            })),
          );
          this.inspecting.set(false);
        },
        error: (err: unknown) => {
          this.inspectError.set(errorMessage(err, 'Die Dateien konnten nicht geprüft werden.'));
          this.inspecting.set(false);
        },
      });
  }

  setType(fileKey: number, type: DocumentType): void {
    this.groups.update((groups) =>
      groups.map((g) => ({
        ...g,
        files: g.files.map((f) => (f.key === fileKey ? { ...f, type } : f)),
      })),
    );
  }

  /** Verschiebt eine Datei zu einer anderen Person oder (ohne Ziel) in eine neue, eigene Bewerbung. */
  moveFile(fileKey: number, targetGroupId: number | null): void {
    const file = this.groups()
      .flatMap((g) => g.files)
      .find((f) => f.key === fileKey);
    if (!file) {
      return;
    }
    let groups = this.groups().map((g) => ({
      ...g,
      files: g.files.filter((f) => f.key !== fileKey),
    }));
    if (targetGroupId === null) {
      groups.push({ id: this.nextId++, files: [file], match: 'NONE', needsReview: false });
    } else {
      groups = groups.map((g) =>
        g.id === targetGroupId ? { ...g, files: [...g.files, file], needsReview: false } : g,
      );
    }
    this.groups.set(groups.filter((g) => g.files.length > 0));
  }

  removeFile(fileKey: number): void {
    this.groups.update((groups) =>
      groups
        .map((g) => ({ ...g, files: g.files.filter((f) => f.key !== fileKey) }))
        .filter((g) => g.files.length > 0),
    );
  }

  discardDraft(): void {
    this.groups.set([]);
    this.inspectError.set(null);
  }

  /** Lädt je Person eine Bewerbung hoch (mit allen Unterlagen) und startet die KI-Analyse. */
  submit(jobId: number): void {
    if (!this.canSubmit()) {
      return;
    }
    for (const group of this.groups()) {
      const pick = (type: DocumentType) => group.files.find((f) => f.type === type)?.file ?? null;
      const resume = pick('RESUME');
      if (!resume) {
        continue;
      }
      const item: UploadItem = {
        id: this.nextId++,
        label: groupLabel(group),
        fileNames: group.files.map((f) => f.file.name),
        sizeBytes: group.files.reduce((sum, f) => sum + f.file.size, 0),
        documents: {
          resume,
          skillProfile: pick('SKILL_PROFILE'),
          coverLetter: pick('COVER_LETTER'),
        },
        stage: 'uploading',
        progress: 0,
        score: null,
        error: null,
        retryable: false,
        applicationId: null,
      };
      this.uploads.update((list) => [...list, item]);
      this.run(item.id, this.uploadAndAnalyze(jobId, item));
    }
    this.groups.set([]);
  }

  /** Erneut versuchen: nur die Analyse, falls die Bewerbung bereits angelegt ist, sonst den ganzen Upload. */
  retry(jobId: number, itemId: number): void {
    const item = this.uploads().find((u) => u.id === itemId);
    if (!item || item.stage !== 'error' || !item.retryable) {
      return;
    }
    this.update(itemId, {
      error: null,
      stage: item.applicationId ? 'analyzing' : 'uploading',
      progress: 0,
    });
    const work = item.applicationId
      ? this.analyzeStep(itemId, item.applicationId)
      : this.uploadAndAnalyze(jobId, item);
    this.run(itemId, work);
  }

  clearFinished(): void {
    this.uploads.update((list) => list.filter((u) => u.stage !== 'done' && u.stage !== 'error'));
  }

  private addError(file: File, message: string): void {
    this.uploads.update((list) => [
      ...list,
      {
        id: this.nextId++,
        label: file.name,
        fileNames: [file.name],
        sizeBytes: file.size,
        documents: null,
        stage: 'error',
        progress: 100,
        score: null,
        error: message,
        retryable: false,
        applicationId: null,
      },
    ]);
  }

  private uploadAndAnalyze(jobId: number, item: UploadItem): Observable<ApplicationDetail> {
    if (!item.documents) {
      throw new Error('Ungültige Dateien werden nicht hochgeladen.');
    }
    return this.api.upload(jobId, item.documents).pipe(
      tap((event) => {
        if (event.type === HttpEventType.UploadProgress) {
          const ratio = event.total ? event.loaded / event.total : 0;
          this.update(
            item.id,
            ratio >= 1 ? this.stage('uploaded') : { progress: Math.round(15 * ratio) },
          );
        }
      }),
      filter((event) => event.type === HttpEventType.Response),
      map((event) => (event.type === HttpEventType.Response ? event.body : null)),
      filter((body): body is ApplicationSummary => body !== null),
      tap((summary) => {
        this.update(item.id, { ...this.stage('extracted'), applicationId: summary.id });
        this.pipeline.upsert(summary);
      }),
      delay(450),
      switchMap((summary) => this.analyzeStep(item.id, summary.id)),
    );
  }

  private analyzeStep(itemId: number, applicationId: number): Observable<ApplicationDetail> {
    this.update(itemId, this.stage('analyzing'));
    return this.api.analyze(applicationId);
  }

  private run(itemId: number, work: Observable<ApplicationDetail>): void {
    work.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (detail) => {
        this.update(itemId, {
          ...this.stage('done'),
          score: detail.analysis?.overallScore ?? null,
        });
        this.pipeline.upsert(toSummary(detail));
      },
      error: (err: unknown) => {
        const current = this.uploads().find((u) => u.id === itemId);
        const message = current?.applicationId
          ? 'Analyse fehlgeschlagen – erneut versuchen'
          : errorMessage(err, 'Upload fehlgeschlagen – erneut versuchen');
        this.update(itemId, { stage: 'error', progress: 100, error: message, retryable: true });
      },
    });
  }

  private stage(stage: keyof typeof STAGE_PROGRESS): Partial<UploadItem> {
    return { stage, progress: STAGE_PROGRESS[stage] };
  }

  private update(itemId: number, changes: Partial<UploadItem>): void {
    this.uploads.update((list) => list.map((u) => (u.id === itemId ? { ...u, ...changes } : u)));
  }
}
