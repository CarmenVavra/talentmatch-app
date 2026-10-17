import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';

/** Für angemeldete Benutzer:innen ohne Rolle HR-Manager (Bewerber-Portal ist noch nicht gestaltet). */
@Component({
  selector: 'app-forbidden',
  imports: [RouterLink],
  template: `
    <section class="forbidden">
      <div class="kicker">Kein Zugriff</div>
      <h1>Dieser Bereich ist HR-Manager:innen vorbehalten</h1>
      <p class="muted">
        Sie sind als {{ auth.user()?.firstName }} {{ auth.user()?.lastName }} angemeldet. Das
        Bewerber-Portal ist noch nicht verfügbar.
      </p>
      <div class="actions">
        <a class="btn btn-secondary btn-lg" routerLink="/account/password">Passwort ändern</a>
        <button type="button" class="btn btn-primary btn-lg" (click)="auth.logout()">
          Abmelden
        </button>
      </div>
    </section>
  `,
  styles: `
    .forbidden {
      display: flex;
      flex-direction: column;
      gap: 12px;
      padding-top: 24px;
      border-top: 2px solid var(--tm-line-strong);
      max-width: 720px;
    }
    h1 {
      font-size: clamp(28px, 3vw, 38px);
      margin: 0;
    }
    .actions {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ForbiddenComponent {
  protected readonly auth = inject(AuthService);
}
