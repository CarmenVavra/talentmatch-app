import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

import { AuthService } from '../../core/auth/auth.service';
import { errorMessage, fieldErrors } from '../../core/util/http-error';
import { ICONS } from '../../shared/icons';
import { LogoComponent } from '../../shared/logo.component';
import { passwordsMatch } from './change-password.component';

/** Selbstregistrierung für Bewerber:innen (Konto erhält immer die Rolle Bewerber:in). */
@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink, LucideAngularModule, LogoComponent],
  templateUrl: './register.component.html',
  styleUrl: './auth-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegisterComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly icons = ICONS;

  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly serverErrors = signal<Record<string, string>>({});

  protected readonly form = inject(NonNullableFormBuilder).group(
    {
      firstName: ['', [Validators.required, Validators.maxLength(100)]],
      lastName: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email]],
      newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]],
      confirmPassword: ['', Validators.required],
    },
    { validators: passwordsMatch },
  );

  protected invalid(name: 'firstName' | 'lastName' | 'email' | 'newPassword'): boolean {
    const control = this.form.controls[name];
    return control.touched && control.invalid;
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    this.serverErrors.set({});
    const value = this.form.getRawValue();
    this.auth
      .register({
        firstName: value.firstName.trim(),
        lastName: value.lastName.trim(),
        email: value.email.trim(),
        password: value.newPassword,
      })
      .subscribe({
        next: () => void this.router.navigateByUrl('/portal/jobs'),
        error: (err: unknown) => {
          this.submitting.set(false);
          this.serverErrors.set(fieldErrors(err));
          this.error.set(errorMessage(err, 'Die Registrierung ist fehlgeschlagen.'));
        },
      });
  }
}
