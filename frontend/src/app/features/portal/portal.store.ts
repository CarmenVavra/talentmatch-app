import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { ApplicantApiService } from '../../core/api/applicant-api.service';
import { AuthService } from '../../core/auth/auth.service';
import { ApplicationDocuments, MyApplication, OpenJob } from '../../core/models/api.models';
import { errorMessage } from '../../core/util/http-error';

/** Zustand des Bewerber-Portals (offene Stellen und eigene Bewerbungen). */
@Injectable({ providedIn: 'root' })
export class PortalStore {
  private readonly api = inject(ApplicantApiService);
  private readonly auth = inject(AuthService);
  /** Konto, zu dem die geladenen Daten gehören – verhindert, dass nach einem Kontowechsel fremde Daten aufblitzen. */
  private owner: string | null = null;

  readonly jobs = signal<OpenJob[]>([]);
  readonly applications = signal<MyApplication[]>([]);
  readonly jobsLoading = signal(false);
  readonly jobsLoaded = signal(false);
  readonly applicationsLoading = signal(false);
  readonly applicationsLoaded = signal(false);
  readonly error = signal<string | null>(null);
  /** Laden einer einzelnen Stelle (Detailseite). */
  readonly jobLoading = signal(false);
  readonly jobError = signal<string | null>(null);

  /** Stelle aus dem geladenen Bestand (z. B. für die Detailseite). */
  job(jobId: number): OpenJob | null {
    return this.jobs().find((j) => j.id === jobId) ?? null;
  }

  loadJobs(): void {
    this.resetIfOtherUser();
    this.jobsLoading.set(true);
    this.error.set(null);
    this.api.openJobs().subscribe({
      next: (jobs) => {
        this.jobs.set(jobs);
        this.jobsLoaded.set(true);
        this.jobsLoading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(errorMessage(err, 'Die Stellen konnten nicht geladen werden.'));
        this.jobsLoading.set(false);
      },
    });
  }

  /**
   * Lädt eine Stelle frisch (auch bei Direktaufruf der Detailseite) und übernimmt sie in den Bestand,
   * damit eine Bewerbung aus der Detailseite sofort als „beworben“ erscheint.
   */
  loadJob(jobId: number): void {
    this.resetIfOtherUser();
    this.jobLoading.set(true);
    this.jobError.set(null);
    this.api.openJob(jobId).subscribe({
      next: (job) => {
        this.jobs.update((list) =>
          list.some((j) => j.id === job.id)
            ? list.map((j) => (j.id === job.id ? job : j))
            : [...list, job],
        );
        this.jobLoading.set(false);
      },
      error: (err: unknown) => {
        this.jobError.set(
          err instanceof HttpErrorResponse && err.status === 404
            ? 'Diese Stelle gibt es nicht (mehr).'
            : errorMessage(err, 'Die Stelle konnte nicht geladen werden.'),
        );
        this.jobLoading.set(false);
      },
    });
  }

  loadApplications(): void {
    this.resetIfOtherUser();
    this.applicationsLoading.set(true);
    this.error.set(null);
    this.api.myApplications().subscribe({
      next: (applications) => {
        this.applications.set(applications);
        this.applicationsLoaded.set(true);
        this.applicationsLoading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(errorMessage(err, 'Ihre Bewerbungen konnten nicht geladen werden.'));
        this.applicationsLoading.set(false);
      },
    });
  }

  /** Reicht eine Bewerbung ein und markiert die Stelle als „beworben“. */
  apply(jobId: number, documents: ApplicationDocuments): Observable<MyApplication> {
    return this.api.apply(jobId, documents).pipe(
      tap((application) => {
        this.jobs.update((list) => list.map((j) => (j.id === jobId ? { ...j, applied: true } : j)));
        this.applications.update((list) => [application, ...list]);
      }),
    );
  }

  /** Zieht eine Bewerbung zurück; die Stelle ist danach wieder bewerbbar. */
  withdraw(application: MyApplication): Observable<void> {
    return this.api.withdraw(application.id).pipe(
      tap(() => {
        this.applications.update((list) => list.filter((a) => a.id !== application.id));
        this.jobs.update((list) =>
          list.map((j) => (j.id === application.jobId ? { ...j, applied: false } : j)),
        );
      }),
    );
  }

  private resetIfOtherUser(): void {
    const current = this.auth.user()?.email ?? null;
    if (current !== this.owner) {
      this.owner = current;
      this.jobs.set([]);
      this.applications.set([]);
      this.jobsLoaded.set(false);
      this.applicationsLoaded.set(false);
    }
  }
}
