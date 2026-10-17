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

import { OpenJob } from '../../core/models/api.models';
import { ICONS } from '../../shared/icons';
import { DeDatePipe } from '../../shared/pipes';
import { ApplyDialogComponent } from './apply-dialog.component';
import { PortalStore } from './portal.store';

/** Portal: vollständige Stellenbeschreibung mit Anforderungen und Bewerbungsmöglichkeit. */
@Component({
  selector: 'app-portal-job-detail',
  imports: [RouterLink, LucideAngularModule, DeDatePipe, ApplyDialogComponent],
  templateUrl: './portal-job-detail.component.html',
  styleUrls: ['./portal.scss', './portal-job-detail.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PortalJobDetailComponent {
  protected readonly store = inject(PortalStore);
  protected readonly icons = ICONS;

  readonly jobId = input.required<number, string>({ transform: numberAttribute });

  protected readonly job = computed<OpenJob | null>(() => this.store.job(this.jobId()));
  protected readonly applying = signal(false);

  constructor() {
    effect(() => {
      const id = this.jobId();
      untracked(() => this.store.loadJob(id));
    });
  }
}
