import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, forkJoin, tap } from 'rxjs';

import { JobApiService } from '../api/job-api.service';
import { CreateJobRequest, DashboardKpis, Job } from '../models/api.models';
import { errorMessage } from '../util/http-error';

export type JobViewMode = 'tiles' | 'table';

const VIEW_MODE_KEY = 'talentmatch.jobs.viewMode';

/**
 * Stellen und Dashboard-Kennzahlen (Signals). Wird beim Öffnen des Dashboards frisch geladen,
 * damit Statuswechsel aus Pipeline und Detail sofort in den Zählern erscheinen.
 */
@Injectable({ providedIn: 'root' })
export class JobStore {
  private readonly api = inject(JobApiService);

  readonly jobs = signal<Job[]>([]);
  readonly kpis = signal<DashboardKpis | null>(null);
  readonly loading = signal(false);
  readonly loaded = signal(false);
  readonly error = signal<string | null>(null);
  readonly selectedJobId = signal<number | null>(null);
  /** Kachel- oder Tabellenansicht – bleibt erhalten, bis der Benutzer sie ändert. */
  readonly viewMode = signal<JobViewMode>(restoreViewMode());

  /** Ziel des Nav-Punkts „Pipeline“: zuletzt geöffnete Stelle, sonst die neueste. */
  readonly pipelineJobId = computed(() => this.selectedJobId() ?? this.jobs()[0]?.id ?? null);

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({ jobs: this.api.list(), kpis: this.api.kpis() }).subscribe({
      next: ({ jobs, kpis }) => {
        this.jobs.set(jobs);
        this.kpis.set(kpis);
        this.loaded.set(true);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(errorMessage(err, 'Die Stellen konnten nicht geladen werden.'));
        this.loading.set(false);
      },
    });
  }

  /** Lädt die Stellen nur, falls noch nicht geschehen (z. B. für den Nav-Link „Pipeline“). */
  ensureLoaded(): void {
    if (!this.loaded() && !this.loading()) {
      this.load();
    }
  }

  create(request: CreateJobRequest): Observable<Job> {
    return this.api.create(request).pipe(
      tap((job) => {
        this.jobs.update((list) => [job, ...list]);
        this.kpis.update((k) =>
          k ? { ...k, openJobs: k.openJobs + 1, newJobsThisMonth: k.newJobsThisMonth + 1 } : k,
        );
      }),
    );
  }

  select(jobId: number): void {
    this.selectedJobId.set(jobId);
  }

  setViewMode(mode: JobViewMode): void {
    this.viewMode.set(mode);
    try {
      localStorage.setItem(VIEW_MODE_KEY, mode);
    } catch {
      // Ohne localStorage gilt die Wahl nur bis zum Neuladen.
    }
  }
}

function restoreViewMode(): JobViewMode {
  try {
    return localStorage.getItem(VIEW_MODE_KEY) === 'table' ? 'table' : 'tiles';
  } catch {
    return 'tiles';
  }
}
