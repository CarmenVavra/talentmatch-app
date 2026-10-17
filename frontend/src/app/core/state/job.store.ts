import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, forkJoin, tap } from 'rxjs';

import { JobApiService } from '../api/job-api.service';
import { CreateJobRequest, DashboardKpis, Job } from '../models/api.models';
import { errorMessage } from '../util/http-error';

export type JobViewMode = 'tiles' | 'table';

const VIEW_MODE_KEY = 'talentmatch.jobs.viewMode';
const PIPELINE_JOB_KEY = 'talentmatch.pipeline.jobId';

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
  /** Zuletzt geöffnete Stelle – bleibt auch nach Ab- und Anmelden im Browser erhalten. */
  readonly selectedJobId = signal<number | null>(restorePipelineJobId());
  /** Kachel- oder Tabellenansicht – bleibt erhalten, bis der Benutzer sie ändert. */
  readonly viewMode = signal<JobViewMode>(restoreViewMode());

  /**
   * Ziel des Nav-Punkts „Pipeline“: zuletzt geöffnete Stelle, sonst die neueste.
   * Eine gemerkte Stelle, die es nicht mehr gibt, wird ignoriert.
   */
  readonly pipelineJobId = computed(() => {
    const selected = this.selectedJobId();
    const jobs = this.jobs();
    const exists = selected !== null && (!this.loaded() || jobs.some((j) => j.id === selected));
    return exists ? selected : (jobs[0]?.id ?? null);
  });

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

  /** Lädt eine einzelne Stelle (z. B. für das Bearbeiten-Formular). */
  fetch(id: number): Observable<Job> {
    return this.api.get(id);
  }

  update(id: number, request: CreateJobRequest): Observable<Job> {
    return this.api
      .update(id, request)
      .pipe(tap((job) => this.jobs.update((list) => list.map((j) => (j.id === id ? job : j)))));
  }

  /** Löscht eine Stelle; die Kennzahlen werden beim nächsten Öffnen des Dashboards neu geladen. */
  delete(id: number): Observable<void> {
    return this.api.delete(id).pipe(
      tap(() => {
        this.jobs.update((list) => list.filter((j) => j.id !== id));
        if (this.selectedJobId() === id) {
          this.selectedJobId.set(null);
          try {
            localStorage.removeItem(PIPELINE_JOB_KEY);
          } catch {
            // nichts zu tun
          }
        }
      }),
    );
  }

  select(jobId: number): void {
    this.selectedJobId.set(jobId);
    try {
      localStorage.setItem(PIPELINE_JOB_KEY, String(jobId));
    } catch {
      // Ohne localStorage gilt die Auswahl nur bis zum Neuladen.
    }
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

function restorePipelineJobId(): number | null {
  try {
    const id = Number(localStorage.getItem(PIPELINE_JOB_KEY));
    return Number.isInteger(id) && id > 0 ? id : null;
  } catch {
    return null;
  }
}

function restoreViewMode(): JobViewMode {
  try {
    return localStorage.getItem(VIEW_MODE_KEY) === 'table' ? 'table' : 'tiles';
  } catch {
    return 'tiles';
  }
}
