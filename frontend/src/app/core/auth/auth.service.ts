import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

import {
  ChangePasswordRequest,
  LoginResponse,
  RegisterRequest,
  Role,
  User,
} from '../models/api.models';

const STORAGE_KEY = 'talentmatch.session';

interface StoredSession {
  token: string;
  expiresAt: string;
  user: User;
}

/**
 * Hält die Sitzung (JWT + Benutzer) als Signal und persistiert sie im localStorage.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly session = signal<StoredSession | null>(this.restore());

  readonly user = computed(() => this.session()?.user ?? null);
  readonly token = computed(() => this.session()?.token ?? null);
  readonly isAuthenticated = computed(() => this.session() !== null);
  readonly isHrManager = computed(() => this.user()?.role === 'HR_MANAGER');
  readonly initials = computed(() => {
    const user = this.user();
    return user ? `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase() : '';
  });

  /** Startseite je Rolle: HR → Stellenübersicht, Bewerber:in → Portal. */
  readonly homeUrl = computed(() => (this.isHrManager() ? '/jobs' : '/portal/jobs'));

  login(email: string, password: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>('/api/auth/login', { email, password })
      .pipe(tap((response) => this.startSession(response)));
  }

  /** Registriert ein Bewerber-Konto und meldet es direkt an. */
  register(request: RegisterRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>('/api/auth/register', request)
      .pipe(tap((response) => this.startSession(response)));
  }

  /** „Passwort vergessen“: Der Server antwortet immer gleich, ob es ein Konto gibt oder nicht. */
  requestPasswordReset(email: string): Observable<void> {
    return this.http.post<void>('/api/auth/password-reset/request', { email });
  }

  /** Setzt über den Einmal-Link aus der E-Mail ein neues Passwort. */
  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>('/api/auth/password-reset/confirm', { token, newPassword });
  }

  private startSession(response: LoginResponse): void {
    const session: StoredSession = {
      token: response.token,
      expiresAt: response.expiresAt,
      user: response.user,
    };
    this.persist(session);
    this.session.set(session);
  }

  changePassword(request: ChangePasswordRequest): Observable<void> {
    return this.http.post<void>('/api/auth/change-password', request);
  }

  hasRole(role: Role): boolean {
    return this.user()?.role === role;
  }

  logout(redirect = true): void {
    this.session.set(null);
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      // localStorage nicht verfügbar – Sitzung ist trotzdem beendet.
    }
    if (redirect) {
      void this.router.navigate(['/login']);
    }
  }

  private persist(session: StoredSession): void {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    } catch {
      // Sitzung bleibt dann nur bis zum Neuladen erhalten.
    }
  }

  private restore(): StoredSession | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) {
        return null;
      }
      const session = JSON.parse(raw) as StoredSession;
      return new Date(session.expiresAt).getTime() > Date.now() ? session : null;
    } catch {
      return null;
    }
  }
}
