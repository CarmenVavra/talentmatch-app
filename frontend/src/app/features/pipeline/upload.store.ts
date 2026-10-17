import { HttpEventType } from '@angular/common/http';
import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, delay, filter, map, switchMap, tap } from 'rxjs';

import { ApplicationApiService } from '../../core/api/application-api.service';
import { ApplicationDetail, ApplicationSummary } from '../../core/models/api.models';
import { toSummary } from '../../core/util/application';
import { errorMessage } from '../../core/util/http-error';
import { PipelineStore } from './pipeline.store';

export const MAX_FILE_BYTES = 10 * 1024 * 1024;

export type UploadStage = 'uploading' | 'uploaded' | 'extracted' | 'analyzing' | 'done' | 'error';

export interface UploadItem {
  id: number;
  file: File;
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

const STAGE_PROGRESS: Record<Exclude<UploadStage, 'uploading' | 'error'>, number> = {
  uploaded: 15,
  extracted: 45,
  analyzing: 75,
  done: 100,
};

/**
 * Upload-Dialog: PDF hochladen → Text extrahiert (Tika) → KI-Analyse → Karte in „KI-analysiert“.
 */
@Injectable()
export class UploadStore {
  private readonly api = inject(ApplicationApiService);
  private readonly pipeline = inject(PipelineStore);
  private readonly destroyRef = inject(DestroyRef);
  private nextId = 1;

  readonly uploads = signal<UploadItem[]>([]);
  readonly hasUploads = computed(() => this.uploads().length > 0);

  addFiles(jobId: number, files: readonly File[]): void {
    for (const file of files) {
      const item: UploadItem = {
        id: this.nextId++,
        file,
        stage: 'uploading',
        progress: 0,
        score: null,
        error: null,
        retryable: false,
        applicationId: null,
      };
      const validationError = validateFile(file);
      if (validationError) {
        this.uploads.update((list) => [
          ...list,
          { ...item, stage: 'error', progress: 100, error: validationError },
        ]);
        continue;
      }
      this.uploads.update((list) => [...list, item]);
      this.run(item.id, this.uploadAndAnalyze(jobId, item));
    }
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

  private uploadAndAnalyze(jobId: number, item: UploadItem): Observable<ApplicationDetail> {
    return this.api.upload(jobId, item.file).pipe(
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

/** Clientseitige Prüfung (der Server prüft zusätzlich Inhalt und Größe). */
export function validateFile(file: File): string | null {
  const isPdf = file.name.toLowerCase().endsWith('.pdf');
  if (!isPdf) {
    return 'Falscher Dateityp – nur PDF-Dateien sind erlaubt.';
  }
  if (file.size > MAX_FILE_BYTES) {
    return 'Die Datei ist größer als 10 MB.';
  }
  return null;
}
