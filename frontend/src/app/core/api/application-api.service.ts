import { HttpClient, HttpEvent } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  ApplicationDetail,
  ApplicationStatus,
  ApplicationSummary,
  DocumentKind,
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

  /** Upload mit Fortschritts-Events (multipart, Feld „file“). */
  upload(jobId: number, file: File): Observable<HttpEvent<ApplicationSummary>> {
    const body = new FormData();
    body.append('file', file, file.name);
    return this.http.post<ApplicationSummary>(`/api/jobs/${jobId}/applications`, body, {
      observe: 'events',
      reportProgress: true,
    });
  }

  /** Original-PDF einer Bewerbungsunterlage. */
  document(id: number, kind: DocumentKind): Observable<Blob> {
    return this.http.get(`/api/applications/${id}/${kind}`, { responseType: 'blob' });
  }
}
