import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApplicationDocuments, MyApplication, OpenJob } from '../models/api.models';

/** Endpunkte des Bewerber-Portals (Rolle APPLICANT). */
@Injectable({ providedIn: 'root' })
export class ApplicantApiService {
  private readonly http = inject(HttpClient);

  openJobs(): Observable<OpenJob[]> {
    return this.http.get<OpenJob[]>('/api/applicant/jobs');
  }

  openJob(jobId: number): Observable<OpenJob> {
    return this.http.get<OpenJob>(`/api/applicant/jobs/${jobId}`);
  }

  myApplications(): Observable<MyApplication[]> {
    return this.http.get<MyApplication[]>('/api/applicant/applications');
  }

  /** Zieht eine eigene Bewerbung zurück – sie wird samt Unterlagen gelöscht. */
  withdraw(applicationId: number): Observable<void> {
    return this.http.delete<void>(`/api/applicant/applications/${applicationId}`);
  }

  /** Löscht das eigene Konto samt allen Bewerbungen (Bestätigung mit dem Passwort). */
  deleteAccount(password: string): Observable<void> {
    return this.http.post<void>('/api/applicant/account/delete', { password });
  }

  /** Multipart: „file“ (Lebenslauf) sowie optional „skillProfile“ und „coverLetter“. */
  apply(jobId: number, documents: ApplicationDocuments): Observable<MyApplication> {
    const body = new FormData();
    body.append('file', documents.resume, documents.resume.name);
    if (documents.skillProfile) {
      body.append('skillProfile', documents.skillProfile, documents.skillProfile.name);
    }
    if (documents.coverLetter) {
      body.append('coverLetter', documents.coverLetter, documents.coverLetter.name);
    }
    return this.http.post<MyApplication>(`/api/applicant/jobs/${jobId}/applications`, body);
  }
}
