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
