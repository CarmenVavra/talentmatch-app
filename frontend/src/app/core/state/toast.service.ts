import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  message: string;
  kind: 'error' | 'success';
}

/** Kurze, sich selbst schließende Hinweise (z. B. fehlgeschlagener Statuswechsel). */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private nextId = 1;
  readonly toasts = signal<Toast[]>([]);

  error(message: string): void {
    this.push(message, 'error');
  }

  success(message: string): void {
    this.push(message, 'success');
  }

  dismiss(id: number): void {
    this.toasts.update((list) => list.filter((t) => t.id !== id));
  }

  private push(message: string, kind: Toast['kind']): void {
    const id = this.nextId++;
    this.toasts.update((list) => [...list, { id, message, kind }]);
    setTimeout(() => this.dismiss(id), 6000);
  }
}
