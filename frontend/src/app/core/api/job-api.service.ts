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

  kpis(): Observable<DashboardKpis> {
    return this.http.get<DashboardKpis>('/api/dashboard/kpis');
  }
}
