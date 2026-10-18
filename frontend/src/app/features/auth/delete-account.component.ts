import { Location } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

import { ApplicantApiService } from '../../core/api/applicant-api.service';
import { AuthService } from '../../core/auth/auth.service';
import { ToastService } from '../../core/state/toast.service';
import { errorMessage } from '../../core/util/http-error';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { ICONS } from '../../shared/icons';

/** Bewerber:innen löschen ihr Konto samt allen Bewerbungen (Bestätigung mit dem Passwort). */
@Component({
  selector: 'app-delete-account',
  imports: [ReactiveFormsModule, RouterLink, LucideAngularModule, ConfirmDialogComponent],
  templateUrl: './delete-account.component.html',
  styleUrl: './auth-page.scss',
  styles: `
    .page {
      display: flex;
      flex-direction: column;
      gap: 24px;
    }
    .panel-box {
      box-shadow: none;
      border-top-width: 2px;
      border-top-color: var(--tm-red-bar);
    }
    .consequences {
      margin: 0;
      padding-left: 20px;
      font-size: 14px;
      line-height: 1.6;
    }
    .actions {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeleteAccountComponent {
  private readonly api = inject(ApplicantApiService);
  private readonly auth = inject(AuthService);
  private readonly toasts = inject(ToastService);
  protected readonly location = inject(Location);
  protected readonly icons = ICONS;

  protected readonly confirmOpen = signal(false);
  protected readonly deleting = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = inject(NonNullableFormBuilder).group({
    password: ['', Validators.required],
  });

  protected askConfirmation(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.error.set(null);
    this.confirmOpen.set(true);
  }

  protected deleteAccount(): void {
    this.deleting.set(true);
    this.api.deleteAccount(this.form.getRawValue().password).subscribe({
      next: () => {
        this.toasts.success('Ihr Konto und alle Ihre Bewerbungen wurden gelöscht.');
        this.auth.logout();
      },
      error: (err: unknown) => {
        this.deleting.set(false);
        this.confirmOpen.set(false);
        this.error.set(errorMessage(err, 'Das Konto konnte nicht gelöscht werden.'));
      },
    });
  }
}
