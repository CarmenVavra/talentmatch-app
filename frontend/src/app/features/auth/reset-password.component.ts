import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

import { AuthService } from '../../core/auth/auth.service';
import { errorMessage } from '../../core/util/http-error';
import { ICONS } from '../../shared/icons';
import { LogoComponent } from '../../shared/logo.component';
import { passwordsMatch } from './change-password.component';

/** Neues Passwort über den Einmal-Link aus der E-Mail festlegen (Token als Query-Parameter). */
@Component({
  selector: 'app-reset-password',
  imports: [ReactiveFormsModule, RouterLink, LucideAngularModule, LogoComponent],
  templateUrl: './reset-password.component.html',
  styleUrl: './auth-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ResetPasswordComponent {
  private readonly auth = inject(AuthService);
  protected readonly icons = ICONS;

  /** Token aus dem Link (?token=…). */
  readonly token = input<string>();

  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly done = signal(false);

  protected readonly form = inject(NonNullableFormBuilder).group(
    {
      newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]],
      confirmPassword: ['', Validators.required],
    },
    { validators: passwordsMatch },
  );

  protected submit(): void {
    const token = this.token();
    if (!token || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    this.auth.resetPassword(token, this.form.getRawValue().newPassword).subscribe({
      next: () => {
        this.submitting.set(false);
        this.done.set(true);
      },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.error.set(errorMessage(err, 'Das Passwort konnte nicht geändert werden.'));
      },
    });
  }
}
