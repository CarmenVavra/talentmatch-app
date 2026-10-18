import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  input,
  output,
  viewChild,
} from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';

import { ICONS } from './icons';

/**
 * Bestätigungsdialog für folgenreiche Aktionen (z. B. Löschen). Der Inhalt wird per Content Projection
 * übergeben; während {@link busy} ist der Dialog gesperrt.
 */
@Component({
  selector: 'app-confirm-dialog',
  imports: [LucideAngularModule],
  templateUrl: './confirm-dialog.component.html',
  styleUrl: './confirm-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown.escape)': 'cancel()' },
})
export class ConfirmDialogComponent implements AfterViewInit {
  protected readonly icons = ICONS;

  readonly title = input.required<string>();
  readonly confirmLabel = input('Bestätigen');
  readonly busyLabel = input('Bitte warten …');
  readonly danger = input(false);
  readonly busy = input(false);
  /** Bestätigen gesperrt, z. B. solange eine Auswahl im Dialog ungültig ist. */
  readonly confirmDisabled = input(false);

  readonly confirmed = output<void>();
  readonly cancelled = output<void>();

  private readonly dialog = viewChild.required<ElementRef<HTMLElement>>('dialog');

  ngAfterViewInit(): void {
    this.dialog().nativeElement.focus();
  }

  protected cancel(): void {
    if (!this.busy()) {
      this.cancelled.emit();
    }
  }

  protected onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.cancel();
    }
  }
}
