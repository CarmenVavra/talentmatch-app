import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { ConfirmDialogComponent } from './confirm-dialog.component';

@Component({
  imports: [ConfirmDialogComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-confirm-dialog
      title="Stelle löschen?"
      confirmLabel="Endgültig löschen"
      [danger]="true"
      [busy]="busy()"
      (confirmed)="confirmed = confirmed + 1"
      (cancelled)="cancelled = cancelled + 1"
    >
      <p class="projected">Details zur Aktion</p>
    </app-confirm-dialog>
  `,
})
class HostComponent {
  readonly busy = signal(false);
  confirmed = 0;
  cancelled = 0;
}

describe('ConfirmDialogComponent', () => {
  async function render() {
    const fixture = TestBed.createComponent(HostComponent);
    await fixture.whenStable();
    return { fixture, root: fixture.nativeElement as HTMLElement, host: fixture.componentInstance };
  }

  it('shows title and projected content as an alert dialog', async () => {
    const { root } = await render();
    expect(root.querySelector('[role="alertdialog"]')).not.toBeNull();
    expect(root.querySelector('.dialog-title')?.textContent).toContain('Stelle löschen?');
    expect(root.querySelector('.projected')?.textContent).toContain('Details zur Aktion');
    expect(root.querySelector('.confirm')?.textContent).toContain('Endgültig löschen');
  });

  it('emits confirmed and cancelled', async () => {
    const { root, host } = await render();
    (root.querySelector('.confirm') as HTMLButtonElement).click();
    (root.querySelector('.cancel') as HTMLButtonElement).click();
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    expect(host.confirmed).toBe(1);
    expect(host.cancelled).toBe(2);
  });

  it('cannot be cancelled while busy', async () => {
    const { fixture, root, host } = await render();
    host.busy.set(true);
    await fixture.whenStable();
    expect((root.querySelector('.confirm') as HTMLButtonElement).disabled).toBeTrue();
    expect(root.querySelector('.confirm')?.textContent).toContain('Bitte warten');
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    expect(host.cancelled).toBe(0);
  });
});
