import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { Analysis } from '../../core/models/api.models';
import { scoreLabel, scoreTone } from '../../core/util/score';

interface SubScore {
  label: string;
  value: number;
  color: string;
}

/** KI-Match-Score mit Urteil, 20-Segment-Balken und Teil-Scores. */
@Component({
  selector: 'app-score-panel',
  templateUrl: './score-panel.component.html',
  styleUrl: './score-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ScorePanelComponent {
  readonly analysis = input<Analysis | null>(null);

  protected readonly score = computed(() => this.analysis()?.overallScore ?? null);
  protected readonly tone = computed(() => scoreTone(this.score()));
  protected readonly label = computed(() => scoreLabel(this.score()));

  /** 20 Zellen à 5 Punkte. */
  protected readonly cells = computed(() => {
    const filled = Math.round((this.score() ?? 0) / 5);
    const bar = this.tone().bar;
    return Array.from({ length: 20 }, (_, i) => (i < filled ? bar : '#E3E6E8'));
  });

  protected readonly subScores = computed<SubScore[]>(() => {
    const a = this.analysis();
    if (!a) {
      return [];
    }
    return [
      { label: 'Skills', value: a.skillsScore },
      { label: 'Erfahrung', value: a.experienceScore },
      { label: 'Ausbildung', value: a.educationScore },
      { label: 'Sprachen', value: a.languagesScore },
    ].map((s) => ({ ...s, color: scoreTone(s.value).bar }));
  });
}
