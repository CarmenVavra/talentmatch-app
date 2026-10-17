import { CdkDrag, CdkDragDrop, CdkDropList, CdkDropListGroup } from '@angular/cdk/drag-drop';
import {
  ChangeDetectionStrategy,
  Component,
  effect,
  inject,
  input,
  numberAttribute,
  signal,
  untracked,
} from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

import { ApplicationStatus, ApplicationSummary } from '../../core/models/api.models';
import { ICONS } from '../../shared/icons';
import { SegmentOption, SegmentedControlComponent } from '../../shared/segmented-control.component';
import { CandidateCardComponent } from './candidate-card.component';
import { MinScore, PipelineStore } from './pipeline.store';
import { UploadDialogComponent } from './upload-dialog.component';
import { UploadStore } from './upload.store';

@Component({
  selector: 'app-pipeline-board',
  imports: [
    RouterLink,
    LucideAngularModule,
    CdkDropListGroup,
    CdkDropList,
    CdkDrag,
    SegmentedControlComponent,
    CandidateCardComponent,
    UploadDialogComponent,
  ],
  providers: [PipelineStore, UploadStore],
  templateUrl: './pipeline-board.component.html',
  styleUrl: './pipeline-board.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PipelineBoardComponent {
  protected readonly store = inject(PipelineStore);
  private readonly router = inject(Router);
  protected readonly icons = ICONS;

  /** Routen-Parameter :jobId */
  readonly jobId = input.required<number, string>({ transform: numberAttribute });

  protected readonly uploadOpen = signal(false);
  protected readonly hoverColumn = signal<ApplicationStatus | null>(null);
  protected readonly skeletonColumns = [1, 2, 3, 4, 5];
  private dragging = false;

  protected readonly scoreOptions: SegmentOption<MinScore>[] = [
    { value: 0, label: 'Alle' },
    { value: 50, label: '≥ 50' },
    { value: 75, label: '≥ 75' },
  ];

  constructor() {
    effect(() => {
      const id = this.jobId();
      untracked(() => this.store.load(id));
    });
  }

  protected onDrop(
    event: CdkDragDrop<ApplicationStatus, ApplicationStatus, ApplicationSummary>,
  ): void {
    this.hoverColumn.set(null);
    if (event.previousContainer !== event.container) {
      this.store.moveTo(event.item.data.id, event.container.data);
    }
  }

  protected onDragStarted(): void {
    this.dragging = true;
  }

  protected onDragEnded(): void {
    this.hoverColumn.set(null);
    // Den Klick, der direkt auf ein Drag-Ende folgt, nicht als „Karte öffnen“ werten.
    setTimeout(() => (this.dragging = false));
  }

  protected open(application: ApplicationSummary): void {
    if (!this.dragging) {
      void this.router.navigate(['/applications', application.id]);
    }
  }
}
