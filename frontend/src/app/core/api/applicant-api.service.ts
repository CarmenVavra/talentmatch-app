import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { MyApplication, OpenJob } from '../models/api.models';

/** Endpunkte des Bewerber-Portals (Rolle APPLICANT). */
@Injectable({ providedIn: 'root' })
export class ApplicantApiService {
  private readonly http = inject(HttpClient);

  openJobs(): Observable<OpenJob[]> {
    return this.http.get<OpenJob[]>('/api/applicant/jobs');
  }

  myApplications(): Observable<MyApplication[]> {
    return this.http.get<MyApplication[]>('/api/applicant/applications');
  }

  apply(jobId: number, file: File): Observable<MyApplication> {
    const body = new FormData();
    body.append('file', file, file.name);
    return this.http.post<MyApplication>(`/api/applicant/jobs/${jobId}/applications`, body);
  }
}
