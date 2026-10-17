import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin } from 'rxjs';

import { ApplicationApiService } from '../../core/api/application-api.service';
import { JobApiService } from '../../core/api/job-api.service';
import { ApplicationStatus, ApplicationSummary, Job } from '../../core/models/api.models';
import { JobStore } from '../../core/state/job.store';
import { ToastService } from '../../core/state/toast.service';
import { toSummary } from '../../core/util/application';
import { errorMessage } from '../../core/util/http-error';
import { STATUS_COLUMNS, StatusMeta, compareByScore } from '../../core/util/score';

export type MinScore = 0 | 50 | 75;

export interface PipelineColumn {
  meta: StatusMeta;
  cards: ApplicationSummary[];
}

/** Zustand der Kanban-Pipeline einer Stelle (pro Pipeline-Ansicht bereitgestellt). */
@Injectable()
export class PipelineStore {
  private readonly api = inject(ApplicationApiService);
  private readonly jobApi = inject(JobApiService);
  private readonly jobStore = inject(JobStore);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly job = signal<Job | null>(null);
  readonly applications = signal<ApplicationSummary[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly query = signal('');
  readonly minScore = signal<MinScore>(0);
  readonly analyzing = signal<ReadonlySet<number>>(new Set());

  readonly total = computed(() => this.applications().length);

  /** Spalten gefiltert (Suche, Mindest-Score – nicht analysierte bleiben sichtbar) und sortiert. */
  readonly columns = computed<PipelineColumn[]>(() => {
    const query = this.query().trim().toLowerCase();
    const min = this.minScore();
    const visible = this.applications().filter((app) => {
      const haystack =
        `${app.candidate.firstName} ${app.candidate.lastName} ${app.skills.join(' ')}`.toLowerCase();
      const matchesQuery = !query || haystack.includes(query);
      const matchesScore = min === 0 || app.overallScore === null || app.overallScore >= min;
      return matchesQuery && matchesScore;
    });
    return STATUS_COLUMNS.map((meta) => ({
      meta,
      cards: visible
        .filter((app) => app.status === meta.key)
        .sort((a, b) => compareByScore(a.overallScore, b.overallScore)),
    }));
  });

  load(jobId: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.jobStore.select(jobId);
    forkJoin({ job: this.jobApi.get(jobId), applications: this.api.listForJob(jobId) })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ job, applications }) => {
          this.job.set(job);
          this.applications.set(applications);
          this.loading.set(false);
        },
        error: (err: unknown) => {
          this.error.set(errorMessage(err, 'Die Pipeline konnte nicht geladen werden.'));
          this.loading.set(false);
        },
      });
  }

  /** Statuswechsel per Drag & Drop – optimistisch, bei Fehler zurückrollen. */
  moveTo(applicationId: number, status: ApplicationStatus): void {
    const previous = this.applications().find((a) => a.id === applicationId);
    if (!previous || previous.status === status) {
      return;
    }
    this.patch(applicationId, { status });
    this.api
      .updateStatus(applicationId, status)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) => this.upsert(updated),
        error: (err: unknown) => {
          this.patch(applicationId, { status: previous.status });
          this.toast.error(errorMessage(err, 'Der Status konnte nicht geändert werden.'));
        },
      });
  }

  analyze(applicationId: number): void {
    if (this.analyzing().has(applicationId)) {
      return;
    }
    this.setAnalyzing(applicationId, true);
    this.api
      .analyze(applicationId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (detail) => {
          this.upsert(toSummary(detail));
          this.setAnalyzing(applicationId, false);
        },
        error: (err: unknown) => {
          this.setAnalyzing(applicationId, false);
          this.toast.error(
            `Analyse fehlgeschlagen – erneut versuchen. ${errorMessage(err, '')}`.trim(),
          );
        },
      });
  }

  isAnalyzing(applicationId: number): boolean {
    return this.analyzing().has(applicationId);
  }

  /** Fügt eine Bewerbung hinzu oder ersetzt sie (z. B. nach Upload oder Analyse). */
  upsert(summary: ApplicationSummary): void {
    if (summary.jobPositionId !== this.job()?.id) {
      return;
    }
    this.applications.update((list) =>
      list.some((a) => a.id === summary.id)
        ? list.map((a) => (a.id === summary.id ? summary : a))
        : [summary, ...list],
    );
  }

  private patch(applicationId: number, changes: Partial<ApplicationSummary>): void {
    this.applications.update((list) =>
      list.map((a) => (a.id === applicationId ? { ...a, ...changes } : a)),
    );
  }

  private setAnalyzing(applicationId: number, value: boolean): void {
    this.analyzing.update((current) => {
      const next = new Set(current);
      if (value) {
        next.add(applicationId);
      } else {
        next.delete(applicationId);
      }
      return next;
    });
  }
}
