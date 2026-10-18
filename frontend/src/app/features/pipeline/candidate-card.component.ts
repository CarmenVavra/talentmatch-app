import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';

import { ApplicationSummary } from '../../core/models/api.models';
import { positionLabel } from '../../core/util/application';
import { scoreLabel, scoreTone } from '../../core/util/score';
import { talentPoolLabel } from '../../core/util/talent-pool';
import { ICONS } from '../../shared/icons';
import { DayMonthPipe } from '../../shared/pipes';

/** Bewerber-Karte im Kanban-Board. */
@Component({
  selector: 'app-candidate-card',
  imports: [LucideAngularModule, DayMonthPipe],
  templateUrl: './candidate-card.component.html',
  styleUrl: './candidate-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CandidateCardComponent {
  readonly application = input.required<ApplicationSummary>();
  readonly analyzing = input(false);
  readonly compact = input(false);
  readonly analyze = output<number>();

  protected readonly icons = ICONS;
  protected readonly poolLabel = talentPoolLabel;
  protected readonly tone = computed(() => scoreTone(this.application().overallScore));
  protected readonly score = computed(() => scoreLabel(this.application().overallScore));
  protected readonly pending = computed(() => this.application().overallScore === null);
  protected readonly topSkills = computed(() => this.application().skills.slice(0, 3));
  protected readonly position = computed(() => {
    const c = this.application().candidate;
    return positionLabel(c.currentRole, c.currentCompany);
  });

  protected startAnalysis(event: Event): void {
    // Klick soll nicht die Detailseite öffnen
    event.stopPropagation();
    event.preventDefault();
    this.analyze.emit(this.application().id);
  }
}
