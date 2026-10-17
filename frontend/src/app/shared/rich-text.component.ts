import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { parseBold } from '../core/util/rich-text';

/**
 * Zeigt Text mit `**fett**`-Auszeichnung an. Zeilenumbrüche bestimmt der umgebende Container
 * (z. B. `white-space: pre-line`); gerendert wird ausschließlich Text, kein HTML.
 */
@Component({
  selector: 'app-rich-text',
  // Reine Leerraum-Knoten zwischen den Blöcken entfernt Angular – es entstehen keine Zusatz-Leerzeichen.
  template: `@for (segment of segments(); track $index) {
    @if (segment.bold) {
      <strong>{{ segment.text }}</strong>
    } @else {
      <ng-container>{{ segment.text }}</ng-container>
    }
  }`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RichTextComponent {
  readonly text = input.required<string>();

  protected readonly segments = computed(() => parseBold(this.text()));
}
