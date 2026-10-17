import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { CreateJobRequest, DashboardKpis, Job } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class JobApiService {
  private readonly http = inject(HttpClient);

  list(): Observable<Job[]> {
    return this.http.get<Job[]>('/api/jobs');
  }

  get(id: number): Observable<Job> {
    return this.http.get<Job>(`/api/jobs/${id}`);
  }

  create(request: CreateJobRequest): Observable<Job> {
    return this.http.post<Job>('/api/jobs', request);
  }

  /** Ändert eine Stelle (gleiche Felder wie beim Anlegen). */
  update(id: number, request: CreateJobRequest): Observable<Job> {
    return this.http.put<Job>(`/api/jobs/${id}`, request);
  }

  /** Löscht eine Stelle samt allen Bewerbungen, Unterlagen und KI-Analysen. */
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/jobs/${id}`);
  }

  kpis(): Observable<DashboardKpis> {
    return this.http.get<DashboardKpis>('/api/dashboard/kpis');
  }
}
