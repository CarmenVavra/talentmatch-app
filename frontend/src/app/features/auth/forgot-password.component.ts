import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

import { AuthService } from '../../core/auth/auth.service';
import { errorMessage } from '../../core/util/http-error';
import { ICONS } from '../../shared/icons';
import { LogoComponent } from '../../shared/logo.component';

/** „Passwort vergessen“: fordert einen Link zum Zurücksetzen per E-Mail an. */
@Component({
  selector: 'app-forgot-password',
  imports: [ReactiveFormsModule, RouterLink, LucideAngularModule, LogoComponent],
  templateUrl: './forgot-password.component.html',
  styleUrl: './auth-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ForgotPasswordComponent {
  private readonly auth = inject(AuthService);
  protected readonly icons = ICONS;

  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);
  /** Adresse, an die (falls ein Konto existiert) der Link geschickt wurde. */
  protected readonly sentTo = signal<string | null>(null);

  protected readonly form = inject(NonNullableFormBuilder).group({
    email: ['', [Validators.required, Validators.email]],
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    const email = this.form.getRawValue().email.trim();
    this.auth.requestPasswordReset(email).subscribe({
      next: () => {
        this.submitting.set(false);
        this.sentTo.set(email);
      },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.error.set(errorMessage(err, 'Die Anfrage ist fehlgeschlagen.'));
      },
    });
  }
}
