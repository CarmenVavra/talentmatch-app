import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  effect,
  inject,
  input,
  signal,
  untracked,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { LucideAngularModule } from 'lucide-angular';

import { TalentPoolApiService } from '../../core/api/talent-pool-api.service';
import { PublicTalentPool } from '../../core/models/api.models';
import { errorMessage } from '../../core/util/http-error';
import { ICONS } from '../../shared/icons';
import { LogoComponent } from '../../shared/logo.component';
import { DeDatePipe } from '../../shared/pipes';

type Outcome = 'confirmed' | 'declined';

/**
 * Antwortseite der Evidenz-Anfrage (ohne Anmeldung, Zugang über den persönlichen Link aus der E-Mail).
 * Der Link aus der E-Mail hebt die gewählte Antwort nur hervor – gespeichert wird erst per Klick, damit
 * automatische Link-Prüfungen von E-Mail-Programmen nichts auslösen.
 */
@Component({
  selector: 'app-talent-pool-answer',
  imports: [LucideAngularModule, LogoComponent, DeDatePipe],
  templateUrl: './talent-pool-answer.component.html',
  styleUrls: ['../auth/auth-page.scss', './talent-pool-answer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TalentPoolAnswerComponent {
  private readonly api = inject(TalentPoolApiService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly icons = ICONS;

  /** Routen-Parameter :token */
  readonly token = input.required<string>();
  /** Query-Parameter ?antwort=ja|nein aus dem E-Mail-Link */
  readonly antwort = input<string | undefined>(undefined);

  protected readonly data = signal<PublicTalentPool | null>(null);
  protected readonly loading = signal(true);
  protected readonly invalid = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly busy = signal(false);
  protected readonly outcome = signal<Outcome | null>(null);
  protected readonly confirmWithdraw = signal(false);

  constructor() {
    effect(() => {
      const token = this.token();
      untracked(() => this.load(token));
    });
  }

  protected confirm(): void {
    this.busy.set(true);
    this.error.set(null);
    this.api
      .confirm(this.token())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.data.set(data);
          this.outcome.set('confirmed');
          this.busy.set(false);
        },
        error: (err: unknown) => this.fail(err),
      });
  }

  /** „Nein, danke“ bzw. Widerruf – die Unterlagen werden sofort gelöscht. */
  protected decline(): void {
    this.busy.set(true);
    this.error.set(null);
    this.api
      .decline(this.token())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.outcome.set('declined');
          this.confirmWithdraw.set(false);
          this.busy.set(false);
        },
        error: (err: unknown) => this.fail(err),
      });
  }

  private load(token: string): void {
    this.loading.set(true);
    this.api
      .publicView(token)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.data.set(data);
          this.loading.set(false);
        },
        error: (err: unknown) => {
          this.loading.set(false);
          if (err instanceof HttpErrorResponse && err.status === 404) {
            this.invalid.set(true);
          } else {
            this.error.set(errorMessage(err, 'Die Anfrage konnte nicht geladen werden.'));
          }
        },
      });
  }

  private fail(err: unknown): void {
    this.busy.set(false);
    if (err instanceof HttpErrorResponse && err.status === 409) {
      this.invalid.set(true);
      return;
    }
    this.error.set(errorMessage(err, 'Ihre Antwort konnte nicht gespeichert werden.'));
  }
}
