import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';

import { ICONS } from '../../shared/icons';
import { ApplicationDetailStore } from './application-detail.store';

/** Generierte Interviewfragen als Checkliste, Notizen und „Weitere Fragen generieren“. */
@Component({
  selector: 'app-interview-helper',
  imports: [LucideAngularModule],
  templateUrl: './interview-helper.component.html',
  styleUrl: './interview-helper.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InterviewHelperComponent {
  protected readonly store = inject(ApplicationDetailStore);
  protected readonly icons = ICONS;

  protected readonly total = computed(() => this.store.questions().length);
  protected readonly progress = computed(() =>
    this.total() ? Math.round((this.store.discussedCount() / this.total()) * 100) : 0,
  );
  protected readonly initialNotes = computed(() => this.store.detail()?.interviewNotes ?? '');

  protected readonly saveLabel = computed(() => {
    switch (this.store.notesState()) {
      case 'saving':
        return 'Wird gespeichert …';
      case 'saved':
        return 'Gespeichert';
      case 'error':
        return 'Speichern fehlgeschlagen';
      default:
        return '';
    }
  });
}
