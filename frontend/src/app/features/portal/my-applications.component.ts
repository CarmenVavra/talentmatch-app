import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

import { ApplicantStatus, MyApplication } from '../../core/models/api.models';
import { ToastService } from '../../core/state/toast.service';
import { ApplicantStatusMeta, applicantStatusMeta } from '../../core/util/applicant-status';
import { errorMessage } from '../../core/util/http-error';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
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

/** Bezeichnungen der eingereichten Unterlagen (Lebenslauf, Skill-Profil, Anschreiben). */
export function submittedDocuments(application: MyApplication): string[] {
  return [
    application.resumeFileName ? 'Lebenslauf' : null,
    application.skillProfileFileName ? 'Skill-Profil' : null,
    application.coverLetterFileName ? 'Anschreiben' : null,
  ].filter((label): label is string => label !== null);
}

/** Portal: eigene Bewerbungen mit Status (ohne Match-Score). */
@Component({
  selector: 'app-my-applications',
  imports: [RouterLink, LucideAngularModule, DeDatePipe, ConfirmDialogComponent],
  templateUrl: './my-applications.component.html',
  styleUrl: './portal.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyApplicationsComponent {
  protected readonly store = inject(PortalStore);
  private readonly toasts = inject(ToastService);
  protected readonly icons = ICONS;
  protected readonly skeletons = [1, 2];

  /** Bewerbung, deren Rückzug gerade bestätigt wird. */
  protected readonly withdrawTarget = signal<MyApplication | null>(null);
  protected readonly withdrawing = signal(false);

  constructor() {
    this.store.loadApplications();
  }

  protected meta(status: ApplicantStatus): ApplicantStatusMeta {
    return applicantStatusMeta(status);
  }

  protected documents(application: MyApplication): string[] {
    return submittedDocuments(application);
  }

  protected steps(status: ApplicantStatus): Step[] {
    return statusSteps(status);
  }

  protected withdraw(application: MyApplication): void {
    this.withdrawing.set(true);
    this.store.withdraw(application).subscribe({
      next: () => {
        this.withdrawing.set(false);
        this.withdrawTarget.set(null);
        this.toasts.success(`Ihre Bewerbung als ${application.jobTitle} wurde zurückgezogen.`);
      },
      error: (err: unknown) => {
        this.withdrawing.set(false);
        this.withdrawTarget.set(null);
        this.toasts.error(errorMessage(err, 'Die Bewerbung konnte nicht zurückgezogen werden.'));
      },
    });
  }
}
