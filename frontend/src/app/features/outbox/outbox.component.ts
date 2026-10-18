import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

import { TalentPoolApiService } from '../../core/api/talent-pool-api.service';
import { MailStatus } from '../../core/models/api.models';
import { errorMessage } from '../../core/util/http-error';
import { linkify } from '../../core/util/talent-pool';
import { ICONS } from '../../shared/icons';
import { RelativeTimePipe } from '../../shared/pipes';

const STATUS_LABELS: Record<MailStatus, string> = {
  QUEUED: 'Wird versendet',
  SENT: 'Versendet',
  FAILED: 'Fehlgeschlagen',
  NOT_SENT: 'Nur im Postausgang',
};

/** HR: alle von der App erzeugten E-Mails – ohne Mailserver die einzige Stelle, an der sie landen. */
@Component({
  selector: 'app-outbox',
  imports: [RouterLink, LucideAngularModule, RelativeTimePipe],
  templateUrl: './outbox.component.html',
  styleUrl: './outbox.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OutboxComponent {
  private readonly api = inject(TalentPoolApiService);
  protected readonly icons = ICONS;
  protected readonly linkify = linkify;

  protected readonly outbox = rxResource({ stream: () => this.api.outbox() });
  protected readonly error = computed(() =>
    this.outbox.error()
      ? errorMessage(this.outbox.error(), 'Der Postausgang konnte nicht geladen werden.')
      : null,
  );

  protected statusLabel(status: MailStatus): string {
    return STATUS_LABELS[status];
  }
}
