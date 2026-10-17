import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

import { ToastService } from '../core/state/toast.service';
import { ICONS } from '../shared/icons';
import { AppHeaderComponent } from './app-header.component';

/** Rahmen aller angemeldeten Seiten: Header, Inhaltsbereich (max. 1440px) und Hinweise. */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, AppHeaderComponent, LucideAngularModule],
  template: `
    <app-header />
    <main class="main">
      <router-outlet />
    </main>
    <div class="toasts" aria-live="polite">
      @for (toast of toasts.toasts(); track toast.id) {
        <div class="toast" [class.success]="toast.kind === 'success'" role="status">
          <span>{{ toast.message }}</span>
          <button
            type="button"
            class="btn btn-icon toast-close"
            aria-label="Hinweis schließen"
            (click)="toasts.dismiss(toast.id)"
          >
            <lucide-icon [img]="icons.x" [size]="14" aria-hidden="true" />
          </button>
        </div>
      }
    </div>
  `,
  styles: `
    .main {
      max-width: 1440px;
      margin: 0 auto;
      padding: clamp(20px, 3vw, 40px) var(--page-padding-x) 64px;
    }
    .toasts {
      position: fixed;
      right: 24px;
      bottom: 24px;
      z-index: 60;
      display: flex;
      flex-direction: column;
      gap: 8px;
      max-width: min(420px, calc(100vw - 48px));
    }
    .toast {
      display: flex;
      align-items: flex-start;
      gap: 8px;
      padding: 10px 8px 10px 14px;
      background: var(--tm-surface);
      border-left: 4px solid var(--tm-red-bar);
      box-shadow: var(--shadow-md);
      font-size: 14px;
    }
    .toast.success {
      border-left-color: var(--tm-green-bar);
    }
    .toast span {
      flex: 1;
      padding-top: 6px;
    }
    .toast-close {
      width: 30px;
      height: 30px;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShellComponent {
  protected readonly toasts = inject(ToastService);
  protected readonly icons = ICONS;
}
