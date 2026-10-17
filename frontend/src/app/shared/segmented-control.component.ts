import { ChangeDetectionStrategy, Component, input, model } from '@angular/core';
import { LucideAngularModule, LucideIconData } from 'lucide-angular';

export interface SegmentOption<T> {
  value: T;
  label: string;
  icon?: LucideIconData;
}

let nextGroupId = 0;

/** Segmented Control (Radiogruppe): 1px-Rahmen, aktive Option blau hinterlegt. */
@Component({
  selector: 'app-segmented-control',
  imports: [LucideAngularModule],
  template: `
    <div class="seg" role="radiogroup" [attr.aria-label]="ariaLabel()">
      @for (option of options(); track option.label) {
        <label class="seg-opt">
          <input
            type="radio"
            [name]="name"
            [checked]="option.value === value()"
            (change)="value.set(option.value)"
          />
          @if (option.icon) {
            <lucide-icon [img]="option.icon" [size]="14" aria-hidden="true" />
          }
          {{ option.label }}
        </label>
      }
    </div>
  `,
  styles: `
    :host {
      display: inline-flex;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SegmentedControlComponent<T> {
  readonly options = input.required<readonly SegmentOption<T>[]>();
  readonly value = model.required<T>();
  readonly ariaLabel = input<string>('');
  protected readonly name = `seg-${nextGroupId++}`;
}
