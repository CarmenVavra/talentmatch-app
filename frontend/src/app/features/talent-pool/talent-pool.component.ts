import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

import { TalentPoolApiService } from '../../core/api/talent-pool-api.service';
import { TalentPoolStatus } from '../../core/models/api.models';
import { errorMessage } from '../../core/util/http-error';
import { talentPoolDetail, talentPoolLabel } from '../../core/util/talent-pool';
import { ICONS } from '../../shared/icons';

/** HR: alle Bewerbungen in Evidenz über alle Stellen – angefragt oder bestätigt. */
@Component({
  selector: 'app-talent-pool',
  imports: [RouterLink, LucideAngularModule],
  templateUrl: './talent-pool.component.html',
  styleUrl: './talent-pool.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TalentPoolComponent {
  private readonly api = inject(TalentPoolApiService);
  protected readonly icons = ICONS;
  protected readonly label = talentPoolLabel;
  protected readonly detail = talentPoolDetail;

  protected readonly entries = rxResource({ stream: () => this.api.list() });
  protected readonly error = computed(() =>
    this.entries.error()
      ? errorMessage(this.entries.error(), 'Die Evidenz konnte nicht geladen werden.')
      : null,
  );
  protected readonly counts = computed(() => {
    const list = this.entries.value() ?? [];
    const count = (status: TalentPoolStatus) =>
      list.filter((e) => e.talentPool.status === status).length;
    return { confirmed: count('CONFIRMED'), requested: count('REQUESTED') };
  });
}
