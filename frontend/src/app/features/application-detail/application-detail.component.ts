import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  numberAttribute,
  signal,
  untracked,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

import { Duplicate, MergeRequest } from '../../core/models/api.models';
import { positionLabel } from '../../core/util/application';
import { statusMeta } from '../../core/util/score';
import { ICONS } from '../../shared/icons';
import { DayMonthPipe, RelativeTimePipe } from '../../shared/pipes';
import { ApplicationDetailStore } from './application-detail.store';
import { InterviewHelperComponent } from './interview-helper.component';
import { MergeDialogComponent, duplicateReason } from './merge-dialog.component';
import { ResumePanelComponent } from './resume-panel.component';
import { ScorePanelComponent } from './score-panel.component';

@Component({
  selector: 'app-application-detail',
  imports: [
    RouterLink,
    LucideAngularModule,
    DayMonthPipe,
    RelativeTimePipe,
    ScorePanelComponent,
    ResumePanelComponent,
    InterviewHelperComponent,
    MergeDialogComponent,
  ],
  providers: [ApplicationDetailStore],
  templateUrl: './application-detail.component.html',
  styleUrl: './application-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ApplicationDetailComponent {
  protected readonly store = inject(ApplicationDetailStore);
  protected readonly icons = ICONS;

  /** Routen-Parameter :id */
  readonly id = input.required<number, string>({ transform: numberAttribute });

  protected readonly detail = this.store.detail;
  /** Duplikat, das gerade zusammengeführt wird (Dialog offen). */
  protected readonly mergeWith = signal<Duplicate | null>(null);
  protected readonly duplicateReason = duplicateReason;
  protected readonly analysis = this.store.analysis;
  protected readonly status = computed(() => {
    const d = this.detail();
    return d ? statusMeta(d.status) : null;
  });
  protected readonly position = computed(() => {
    const c = this.detail()?.candidate;
    return c ? positionLabel(c.currentRole, c.currentCompany) : '';
  });
  protected readonly requirementRatio = computed(() => {
    const a = this.analysis();
    return a
      ? `${a.matchingSkills.length} von ${a.matchingSkills.length + a.missingSkills.length}`
      : '';
  });

  constructor() {
    effect(() => {
      const id = this.id();
      untracked(() => this.store.load(id));
    });
  }

  protected merge(request: MergeRequest): void {
    this.store.merge(request, (success) => {
      if (success) {
        this.mergeWith.set(null);
      }
    });
  }
}
