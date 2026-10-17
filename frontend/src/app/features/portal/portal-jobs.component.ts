import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

import { OpenJob } from '../../core/models/api.models';
import { ICONS } from '../../shared/icons';
import { DeDatePipe } from '../../shared/pipes';
import { RichTextComponent } from '../../shared/rich-text.component';
import { SegmentOption, SegmentedControlComponent } from '../../shared/segmented-control.component';
import { ApplyDialogComponent } from './apply-dialog.component';
import { PortalStore } from './portal.store';

/** Suche über Titel, Abteilung, Ort und Skills; Abteilungsfilter exakt. */
export function filterOpenJobs(
  jobs: readonly OpenJob[],
  query: string,
  department: string,
): OpenJob[] {
  const q = query.trim().toLowerCase();
  return jobs.filter((job) => {
    const haystack =
      `${job.title} ${job.department} ${job.location} ${job.requiredSkills.join(' ')}`.toLowerCase();
    return (department === 'Alle' || job.department === department) && (!q || haystack.includes(q));
  });
}

/** Portal: offene Stellen ansehen und sich bewerben. */
@Component({
  selector: 'app-portal-jobs',
  imports: [
    RouterLink,
    LucideAngularModule,
    SegmentedControlComponent,
    DeDatePipe,
    ApplyDialogComponent,
    RichTextComponent,
  ],
  templateUrl: './portal-jobs.component.html',
  styleUrl: './portal.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PortalJobsComponent {
  protected readonly store = inject(PortalStore);
  protected readonly icons = ICONS;

  protected readonly query = signal('');
  protected readonly department = signal('Alle');
  protected readonly applyingTo = signal<OpenJob | null>(null);
  protected readonly skeletons = [1, 2, 3];

  protected readonly departmentOptions = computed<SegmentOption<string>[]>(() =>
    ['Alle', ...new Set(this.store.jobs().map((j) => j.department))].map((d) => ({
      value: d,
      label: d,
    })),
  );

  protected readonly jobs = computed(() =>
    filterOpenJobs(this.store.jobs(), this.query(), this.department()),
  );

  constructor() {
    this.store.loadJobs();
  }
}
