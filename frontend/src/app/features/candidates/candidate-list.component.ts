import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

import { ApplicationApiService } from '../../core/api/application-api.service';
import { ApplicationStatus, ApplicationSummary } from '../../core/models/api.models';
import { positionLabel } from '../../core/util/application';
import { errorMessage } from '../../core/util/http-error';
import { STATUS_COLUMNS } from '../../core/util/score';
import { ICONS } from '../../shared/icons';
import { DeDatePipe, ScoreLabelPipe, ScoreTonePipe, StatusMetaPipe } from '../../shared/pipes';
import { SegmentOption, SegmentedControlComponent } from '../../shared/segmented-control.component';

type StatusFilter = ApplicationStatus | 'ALL';
type SortKey = 'date' | 'score';

/** Suche über Name, Stelle, Position und Skills; Statusfilter exakt. */
export function filterApplications(
  applications: readonly ApplicationSummary[],
  query: string,
  status: StatusFilter,
): ApplicationSummary[] {
  const q = query.trim().toLowerCase();
  return applications.filter((app) => {
    const c = app.candidate;
    const haystack =
      `${c.firstName} ${c.lastName} ${app.jobTitle} ${c.currentRole ?? ''} ${app.skills.join(' ')}`.toLowerCase();
    return (status === 'ALL' || app.status === status) && (!q || haystack.includes(q));
  });
}

/** Bewerber:innen über alle Stellen (Nav-Punkt „Bewerber:innen“). */
@Component({
  selector: 'app-candidate-list',
  imports: [
    LucideAngularModule,
    SegmentedControlComponent,
    DeDatePipe,
    ScoreLabelPipe,
    ScoreTonePipe,
    StatusMetaPipe,
  ],
  templateUrl: './candidate-list.component.html',
  styleUrl: './candidate-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CandidateListComponent {
  private readonly api = inject(ApplicationApiService);
  private readonly router = inject(Router);
  protected readonly icons = ICONS;

  protected readonly applications = rxResource({ stream: () => this.api.listAll() });
  protected readonly query = signal('');
  protected readonly status = signal<StatusFilter>('ALL');
  protected readonly sort = signal<SortKey>('date');

  protected readonly statusOptions: SegmentOption<StatusFilter>[] = [
    { value: 'ALL', label: 'Alle' },
    ...STATUS_COLUMNS.map((c) => ({ value: c.key, label: c.label })),
  ];
  protected readonly sortOptions: SegmentOption<SortKey>[] = [
    { value: 'date', label: 'Neueste' },
    { value: 'score', label: 'Score' },
  ];

  protected readonly rows = computed(() => {
    const list = filterApplications(this.applications.value() ?? [], this.query(), this.status());
    return this.sort() === 'score'
      ? [...list].sort((a, b) => (b.overallScore ?? -1) - (a.overallScore ?? -1))
      : list;
  });

  protected readonly errorText = computed(() =>
    this.applications.error()
      ? errorMessage(this.applications.error(), 'Die Bewerbungen konnten nicht geladen werden.')
      : null,
  );

  protected position(app: ApplicationSummary): string {
    return positionLabel(app.candidate.currentRole, app.candidate.currentCompany);
  }

  protected open(app: ApplicationSummary): void {
    void this.router.navigate(['/applications', app.id]);
  }
}
