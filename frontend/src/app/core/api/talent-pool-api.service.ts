import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Outbox, PublicTalentPool, TalentPoolEntry } from '../models/api.models';

/** Evidenz (HR-Liste, öffentliche Antwortseite) und Postausgang. */
@Injectable({ providedIn: 'root' })
export class TalentPoolApiService {
  private readonly http = inject(HttpClient);

  list(): Observable<TalentPoolEntry[]> {
    return this.http.get<TalentPoolEntry[]>('/api/talent-pool');
  }

  outbox(): Observable<Outbox> {
    return this.http.get<Outbox>('/api/outbox');
  }

  /** Antwortseite ohne Anmeldung – der Schlüssel aus dem Link genügt. */
  publicView(token: string): Observable<PublicTalentPool> {
    return this.http.get<PublicTalentPool>(`/api/public/talent-pool/${encodeURIComponent(token)}`);
  }

  confirm(token: string): Observable<PublicTalentPool> {
    return this.http.post<PublicTalentPool>(
      `/api/public/talent-pool/${encodeURIComponent(token)}/confirm`,
      {},
    );
  }

  /** „Nein, danke“ bzw. Widerruf: Die Unterlagen werden sofort gelöscht. */
  decline(token: string): Observable<void> {
    return this.http.post<void>(`/api/public/talent-pool/${encodeURIComponent(token)}/decline`, {});
  }
}
