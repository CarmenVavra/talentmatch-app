import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** CSS-Logo: 2×2-Raster aus den vier Kundenfarben (Platzhalter bis zum echten Logo). */
@Component({
  selector: 'app-logo',
  template: `
    <span class="logo" [style.width.px]="size()" [style.height.px]="size()" aria-hidden="true">
      <span style="background: #4d6d9a"></span>
      <span style="background: #86b3d1"></span>
      <span style="background: #99ced3"></span>
      <span style="background: #edb5bf"></span>
    </span>
  `,
  styles: `
    .logo {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 2px;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LogoComponent {
  readonly size = input(22);
}
