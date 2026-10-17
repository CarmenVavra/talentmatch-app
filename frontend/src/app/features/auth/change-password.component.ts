import { Location } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';

import { AuthService } from '../../core/auth/auth.service';
import { errorMessage } from '../../core/util/http-error';
import { ICONS } from '../../shared/icons';

/** Neues Passwort und Wiederholung müssen übereinstimmen. */
export const passwordsMatch: ValidatorFn = (group: AbstractControl): ValidationErrors | null => {
  const next = group.get('newPassword')?.value as string | undefined;
  const confirm = group.get('confirmPassword')?.value as string | undefined;
  return next && confirm && next !== confirm ? { mismatch: true } : null;
};

@Component({
  selector: 'app-change-password',
  imports: [ReactiveFormsModule, LucideAngularModule],
  templateUrl: './change-password.component.html',
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
      border-top-color: var(--tm-line-strong);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChangePasswordComponent {
  private readonly auth = inject(AuthService);
  protected readonly location = inject(Location);
  protected readonly icons = ICONS;
  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly done = signal(false);

  protected readonly form = inject(NonNullableFormBuilder).group(
    {
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]],
      confirmPassword: ['', Validators.required],
    },
    { validators: passwordsMatch },
  );

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    this.done.set(false);
    const { currentPassword, newPassword } = this.form.getRawValue();
    this.auth.changePassword({ currentPassword, newPassword }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.done.set(true);
        this.form.reset();
      },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.error.set(errorMessage(err, 'Das Passwort konnte nicht geändert werden.'));
      },
    });
  }
}
