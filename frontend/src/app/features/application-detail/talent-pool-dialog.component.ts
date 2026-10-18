import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LucideAngularModule } from 'lucide-angular';

import { errorMessage } from '../../core/util/http-error';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { ICONS } from '../../shared/icons';
import { ApplicationDetailStore } from './application-detail.store';

/** Platzhalter, den der Server beim Versand durch die persönlichen Antwort-Links ersetzt. */
export const LINKS_MARKER = '[Antwort-Links]';

/**
 * Absage mit Evidenz-Anfrage: zeigt den Vorschlag für die E-Mail, HR kann Betreff und Text anpassen.
 */
@Component({
  selector: 'app-talent-pool-dialog',
  imports: [ConfirmDialogComponent, LucideAngularModule],
  templateUrl: './talent-pool-dialog.component.html',
  styleUrl: './talent-pool-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TalentPoolDialogComponent implements OnInit {
  protected readonly store = inject(ApplicationDetailStore);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly icons = ICONS;
  protected readonly marker = LINKS_MARKER;

  readonly jobTitle = input.required<string>();
  readonly closed = output<void>();

  protected readonly recipient = signal('');
  protected readonly subject = signal('');
  protected readonly body = signal('');
  protected readonly deliveryEnabled = signal(false);
  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);

  ngOnInit(): void {
    this.store
      .talentPoolPreview()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (mail) => {
          this.recipient.set(mail.recipient);
          this.subject.set(mail.subject);
          this.body.set(mail.body);
          this.deliveryEnabled.set(mail.deliveryEnabled);
          this.loading.set(false);
        },
        error: (err: unknown) => {
          this.loadError.set(
            errorMessage(err, 'Der E-Mail-Vorschlag konnte nicht geladen werden.'),
          );
          this.loading.set(false);
        },
      });
  }

  protected canSend(): boolean {
    return !this.loading() && !this.loadError() && !!this.subject().trim() && !!this.body().trim();
  }

  protected send(): void {
    if (!this.canSend()) {
      return;
    }
    this.store.requestTalentPool(
      { subject: this.subject(), body: this.body() },
      this.deliveryEnabled(),
      (success) => {
        if (success) {
          this.closed.emit();
        }
      },
    );
  }
}
