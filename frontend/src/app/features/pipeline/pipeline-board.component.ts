import { CdkDrag, CdkDragDrop, CdkDropList, CdkDropListGroup } from '@angular/cdk/drag-drop';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  effect,
  inject,
  input,
  numberAttribute,
  signal,
  untracked,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

import { ApplicationStatus, ApplicationSummary } from '../../core/models/api.models';
import { JobStore } from '../../core/state/job.store';
import { ToastService } from '../../core/state/toast.service';
import { errorMessage } from '../../core/util/http-error';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
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
    ConfirmDialogComponent,
  ],
  providers: [PipelineStore, UploadStore],
  templateUrl: './pipeline-board.component.html',
  styleUrl: './pipeline-board.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PipelineBoardComponent {
  protected readonly store = inject(PipelineStore);
  private readonly router = inject(Router);
  private readonly jobStore = inject(JobStore);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly icons = ICONS;

  /** Routen-Parameter :jobId */
  readonly jobId = input.required<number, string>({ transform: numberAttribute });

  protected readonly uploadOpen = signal(false);
  protected readonly deleteOpen = signal(false);
  protected readonly deleting = signal(false);
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

  /** Löscht die Stelle samt Bewerbungen und kehrt zum Dashboard zurück. */
  protected deleteJob(): void {
    const job = this.store.job();
    if (!job || this.deleting()) {
      return;
    }
    this.deleting.set(true);
    this.jobStore
      .delete(job.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.toast.success(`Die Stelle „${job.title}“ wurde gelöscht.`);
          void this.router.navigate(['/jobs']);
        },
        error: (err: unknown) => {
          this.deleting.set(false);
          this.deleteOpen.set(false);
          this.toast.error(errorMessage(err, 'Die Stelle konnte nicht gelöscht werden.'));
        },
      });
  }

  protected open(application: ApplicationSummary): void {
    if (!this.dragging) {
      void this.router.navigate(['/applications', application.id]);
    }
  }
}
