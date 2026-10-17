import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

import { Job } from '../../core/models/api.models';
import { JobStore } from '../../core/state/job.store';
import { ICONS } from '../../shared/icons';
import { DeDatePipe, ScoreLabelPipe, ScoreTonePipe } from '../../shared/pipes';
import { SegmentOption, SegmentedControlComponent } from '../../shared/segmented-control.component';
import { DEPARTMENTS, filterJobs, statusSegments } from './job-filters';

type ViewMode = 'tiles' | 'table';

interface Kpi {
  label: string;
  value: string;
  note: string;
  noteColor: string;
}

@Component({
  selector: 'app-job-dashboard',
  imports: [
    RouterLink,
    LucideAngularModule,
    SegmentedControlComponent,
    ScoreTonePipe,
    ScoreLabelPipe,
    DeDatePipe,
  ],
  templateUrl: './job-dashboard.component.html',
  styleUrl: './job-dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class JobDashboardComponent {
  protected readonly store = inject(JobStore);
  private readonly router = inject(Router);
  protected readonly icons = ICONS;

  protected readonly query = signal('');
  protected readonly department = signal<string>('Alle');
  protected readonly viewMode = signal<ViewMode>('tiles');

  protected readonly departmentOptions: SegmentOption<string>[] = ['Alle', ...DEPARTMENTS].map(
    (d) => ({ value: d, label: d }),
  );
  protected readonly viewOptions: SegmentOption<ViewMode>[] = [
    { value: 'tiles', label: 'Kacheln', icon: ICONS.grid },
    { value: 'table', label: 'Tabelle', icon: ICONS.list },
  ];

  protected readonly jobs = computed(() =>
    filterJobs(this.store.jobs(), this.query(), this.department()).map((job) => ({
      job,
      segments: statusSegments(job),
    })),
  );

  protected readonly kpis = computed<Kpi[]>(() => {
    const k = this.store.kpis();
    if (!k) {
      return [];
    }
    return [
      {
        label: 'Offene Stellen',
        value: String(k.openJobs),
        note: `${k.newJobsThisMonth} neu in diesem Monat`,
        noteColor: '#5F6366',
      },
      {
        label: 'Bewerbungen gesamt',
        value: String(k.totalApplications),
        note: `+${k.applicationsLastWeek} seit letzter Woche`,
        noteColor: '#25603F',
      },
      {
        label: 'Unbearbeitet',
        value: String(k.unprocessed),
        note: 'warten auf KI-Analyse',
        noteColor: '#75560E',
      },
      {
        label: 'Im Interview',
        value: String(k.interviews),
        note: 'Bewerbungen in Phase „Interview“',
        noteColor: '#5F6366',
      },
    ];
  });

  protected readonly skeletons = [1, 2, 3, 4, 5, 6];

  constructor() {
    this.store.load();
  }

  protected open(job: Job): void {
    void this.router.navigate(['/jobs', job.id, 'pipeline']);
  }
}
