import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

import { AuthService } from '../../core/auth/auth.service';
import { errorMessage } from '../../core/util/http-error';
import { ICONS } from '../../shared/icons';
import { LogoComponent } from '../../shared/logo.component';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, LucideAngularModule, LogoComponent],
  templateUrl: './login.component.html',
  styleUrl: './auth-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly icons = ICONS;

  /** Ziel nach erfolgreicher Anmeldung (Query-Parameter, vom authGuard gesetzt). */
  readonly returnUrl = input<string>();

  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = inject(NonNullableFormBuilder).group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    const { email, password } = this.form.getRawValue();
    this.auth.login(email, password).subscribe({
      next: (response) => {
        const fallback = response.user.role === 'HR_MANAGER' ? '/jobs' : '/forbidden';
        const target = this.returnUrl()?.startsWith('/') ? this.returnUrl() : fallback;
        void this.router.navigateByUrl(target ?? fallback);
      },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.error.set(errorMessage(err, 'Die Anmeldung ist fehlgeschlagen.'));
      },
    });
  }
}
