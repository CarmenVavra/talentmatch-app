import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

import { ApplicantStatus } from '../../core/models/api.models';
import { ApplicantStatusMeta, applicantStatusMeta } from '../../core/util/applicant-status';
import { ICONS } from '../../shared/icons';
import { DeDatePipe } from '../../shared/pipes';
import { PortalStore } from './portal.store';

interface Step {
  label: string;
  state: 'done' | 'current' | 'open';
}

/** Fortschritt Eingegangen → Interview → Entscheidung (Zusage/Absage). */
export function statusSteps(status: ApplicantStatus): Step[] {
  if (status === 'REJECTED') {
    // Ob vor der Absage ein Interview stattfand, ist für die Anzeige unerheblich.
    return [
      { label: 'Eingegangen', state: 'done' },
      { label: 'Absage', state: 'current' },
    ];
  }
  const current = applicantStatusMeta(status).step;
  const decision = status === 'OFFER' ? 'Zusage' : 'Entscheidung';
  return ['Eingegangen', 'Interview', decision].map((label, index) => ({
    label,
    state: index < current ? 'done' : index === current ? 'current' : 'open',
  }));
}

/** Portal: eigene Bewerbungen mit Status (ohne Match-Score). */
@Component({
  selector: 'app-my-applications',
  imports: [RouterLink, LucideAngularModule, DeDatePipe],
  templateUrl: './my-applications.component.html',
  styleUrl: './portal.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyApplicationsComponent {
  protected readonly store = inject(PortalStore);
  protected readonly icons = ICONS;
  protected readonly skeletons = [1, 2];

  constructor() {
    this.store.loadApplications();
  }

  protected meta(status: ApplicantStatus): ApplicantStatusMeta {
    return applicantStatusMeta(status);
  }

  protected steps(status: ApplicantStatus): Step[] {
    return statusSteps(status);
  }
}
