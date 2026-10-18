import { HttpClient, HttpEvent } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  ApplicationDetail,
  ApplicationDocuments,
  ApplicationStatus,
  ApplicationSummary,
  DocumentKind,
  Duplicate,
  MergeRequest,
  UploadInspection,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ApplicationApiService {
  private readonly http = inject(HttpClient);

  listForJob(jobId: number): Observable<ApplicationSummary[]> {
    return this.http.get<ApplicationSummary[]>(`/api/jobs/${jobId}/applications`);
  }

  listAll(): Observable<ApplicationSummary[]> {
    return this.http.get<ApplicationSummary[]>('/api/applications');
  }

  get(id: number): Observable<ApplicationDetail> {
    return this.http.get<ApplicationDetail>(`/api/applications/${id}`);
  }

  updateStatus(id: number, status: ApplicationStatus): Observable<ApplicationSummary> {
    return this.http.patch<ApplicationSummary>(`/api/applications/${id}`, { status });
  }

  updateNotes(id: number, notes: string): Observable<void> {
    return this.http.put<void>(`/api/applications/${id}/notes`, { notes });
  }

  /** HR hat die Bewerbung geöffnet – entfernt die „Neu“-Markierung. */
  markSeen(id: number): Observable<void> {
    return this.http.post<void>(`/api/applications/${id}/seen`, {});
  }

  markQuestion(id: number, index: number, discussed: boolean): Observable<void> {
    return this.http.patch<void>(`/api/applications/${id}/questions/${index}`, { discussed });
  }

  analyze(id: number): Observable<ApplicationDetail> {
    return this.http.post<ApplicationDetail>(`/api/applications/${id}/analyze`, {});
  }

  generateQuestions(id: number): Observable<ApplicationDetail> {
    return this.http.post<ApplicationDetail>(`/api/applications/${id}/questions/generate`, {});
  }

  /** Vorabprüfung mehrerer Dateien: Dokumentart und Zuordnung zu Personen (speichert nichts). */
  inspect(jobId: number, files: readonly File[]): Observable<UploadInspection> {
    const body = new FormData();
    for (const file of files) {
      body.append('files', file, file.name);
    }
    return this.http.post<UploadInspection>(`/api/jobs/${jobId}/applications/inspect`, body);
  }

  /** Upload einer Person mit Fortschritts-Events: „file“ (Lebenslauf), optional „skillProfile“, „coverLetter“. */
  upload(
    jobId: number,
    documents: ApplicationDocuments,
  ): Observable<HttpEvent<ApplicationSummary>> {
    const body = new FormData();
    body.append('file', documents.resume, documents.resume.name);
    if (documents.skillProfile) {
      body.append('skillProfile', documents.skillProfile, documents.skillProfile.name);
    }
    if (documents.coverLetter) {
      body.append('coverLetter', documents.coverLetter, documents.coverLetter.name);
    }
    return this.http.post<ApplicationSummary>(`/api/jobs/${jobId}/applications`, body, {
      observe: 'events',
      reportProgress: true,
    });
  }

  /** Andere Bewerbungen derselben Person auf diese Stelle. */
  duplicates(id: number): Observable<Duplicate[]> {
    return this.http.get<Duplicate[]>(`/api/applications/${id}/duplicates`);
  }

  /** Führt eine andere Bewerbung in diese über (die andere wird gelöscht). */
  merge(id: number, request: MergeRequest): Observable<ApplicationDetail> {
    return this.http.post<ApplicationDetail>(`/api/applications/${id}/merge`, request);
  }

  /** Original-PDF einer Bewerbungsunterlage. */
  document(id: number, kind: DocumentKind): Observable<Blob> {
    return this.http.get(`/api/applications/${id}/${kind}`, { responseType: 'blob' });
  }
}
