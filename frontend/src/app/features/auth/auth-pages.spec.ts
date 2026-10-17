import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { FormControl, FormGroup } from '@angular/forms';
import { Router, provideRouter } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { makeUser } from '../../testing/test-data';
import { ChangePasswordComponent, passwordsMatch } from './change-password.component';
import { LoginComponent } from './login.component';
import { RegisterComponent } from './register.component';

function setValue(root: HTMLElement, selector: string, value: string): void {
  const input = root.querySelector(selector) as HTMLInputElement;
  input.value = value;
  input.dispatchEvent(new Event('input'));
}

describe('auth pages', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  describe('LoginComponent', () => {
    it('does not submit an invalid form', async () => {
      const fixture = TestBed.createComponent(LoginComponent);
      await fixture.whenStable();
      (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(
        new Event('submit'),
      );
      await fixture.whenStable();
      http.expectNone('/api/auth/login');
      expect(fixture.nativeElement.textContent).toContain(
        'Bitte eine gültige E-Mail-Adresse eingeben.',
      );
    });

    it('logs in and navigates HR managers to the dashboard', async () => {
      const navigate = spyOn(TestBed.inject(Router), 'navigateByUrl').and.resolveTo(true);
      const fixture = TestBed.createComponent(LoginComponent);
      await fixture.whenStable();
      setValue(fixture.nativeElement, '#email', 'katrin.vogel@talentmatch.de');
      setValue(fixture.nativeElement, '#password', 'secret');
      (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(
        new Event('submit'),
      );

      http.expectOne('/api/auth/login').flush({
        token: 't',
        expiresAt: new Date(Date.now() + 60_000).toISOString(),
        user: makeUser(),
      });
      expect(navigate).toHaveBeenCalledWith('/jobs');
    });

    it('sends applicants to the portal', async () => {
      const navigate = spyOn(TestBed.inject(Router), 'navigateByUrl').and.resolveTo(true);
      const fixture = TestBed.createComponent(LoginComponent);
      await fixture.whenStable();
      setValue(fixture.nativeElement, '#email', 'bewerber@talentmatch.de');
      setValue(fixture.nativeElement, '#password', 'secret');
      (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(
        new Event('submit'),
      );
      http.expectOne('/api/auth/login').flush({
        token: 't',
        expiresAt: new Date(Date.now() + 60_000).toISOString(),
        user: makeUser({ role: 'APPLICANT' }),
      });
      expect(navigate).toHaveBeenCalledWith('/portal/jobs');
    });

    it('shows the server error message', async () => {
      const fixture = TestBed.createComponent(LoginComponent);
      await fixture.whenStable();
      setValue(fixture.nativeElement, '#email', 'katrin.vogel@talentmatch.de');
      setValue(fixture.nativeElement, '#password', 'falsch');
      (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(
        new Event('submit'),
      );
      http
        .expectOne('/api/auth/login')
        .flush(
          { detail: 'E-Mail oder Passwort ist falsch.' },
          { status: 401, statusText: 'Unauthorized' },
        );
      await fixture.whenStable();
      expect(fixture.nativeElement.querySelector('.alert').textContent).toContain(
        'E-Mail oder Passwort ist falsch.',
      );
    });
  });

  describe('ChangePasswordComponent', () => {
    it('validates that both new passwords match', () => {
      const group = new FormGroup({
        newPassword: new FormControl('abcdefgh'),
        confirmPassword: new FormControl('abcdefgX'),
      });
      expect(passwordsMatch(group)).toEqual({ mismatch: true });
      group.patchValue({ confirmPassword: 'abcdefgh' });
      expect(passwordsMatch(group)).toBeNull();
    });

    it('changes the password and confirms success', async () => {
      const fixture = TestBed.createComponent(ChangePasswordComponent);
      await fixture.whenStable();
      setValue(fixture.nativeElement, '#currentPassword', 'alt-passwort');
      setValue(fixture.nativeElement, '#newPassword', 'neues-passwort');
      setValue(fixture.nativeElement, '#confirmPassword', 'neues-passwort');
      (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(
        new Event('submit'),
      );

      const req = http.expectOne('/api/auth/change-password');
      expect(req.request.body).toEqual({
        currentPassword: 'alt-passwort',
        newPassword: 'neues-passwort',
      });
      req.flush(null);
      await fixture.whenStable();
      expect(fixture.nativeElement.textContent).toContain('Das Passwort wurde geändert.');
    });
  });

  describe('RegisterComponent', () => {
    async function fillAndSubmit(confirm = 'geheim-123') {
      const fixture = TestBed.createComponent(RegisterComponent);
      await fixture.whenStable();
      const root = fixture.nativeElement as HTMLElement;
      setValue(root, '#firstName', 'Max');
      setValue(root, '#lastName', 'Mustermann');
      setValue(root, '#email', 'max@example.org');
      setValue(root, '#newPassword', 'geheim-123');
      setValue(root, '#confirmPassword', confirm);
      (root.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
      await fixture.whenStable();
      return fixture;
    }

    it('registers an applicant and opens the portal', async () => {
      const navigate = spyOn(TestBed.inject(Router), 'navigateByUrl').and.resolveTo(true);
      await fillAndSubmit();
      const req = http.expectOne('/api/auth/register');
      expect(req.request.body).toEqual({
        firstName: 'Max',
        lastName: 'Mustermann',
        email: 'max@example.org',
        password: 'geheim-123',
      });
      req.flush({
        token: 't',
        expiresAt: new Date(Date.now() + 60_000).toISOString(),
        user: makeUser({ role: 'APPLICANT' }),
      });
      expect(TestBed.inject(AuthService).hasRole('APPLICANT')).toBeTrue();
      expect(navigate).toHaveBeenCalledWith('/portal/jobs');
    });

    it('does not submit when the passwords differ', async () => {
      const fixture = await fillAndSubmit('anders-123');
      http.expectNone('/api/auth/register');
      expect(fixture.nativeElement.textContent).toContain('Die Passwörter stimmen nicht überein.');
    });

    it('shows a conflict for existing accounts', async () => {
      const fixture = await fillAndSubmit();
      http
        .expectOne('/api/auth/register')
        .flush(
          { detail: 'Für diese E-Mail-Adresse existiert bereits ein Konto.' },
          { status: 409, statusText: 'Conflict' },
        );
      await fixture.whenStable();
      expect(fixture.nativeElement.querySelector('.alert').textContent).toContain(
        'existiert bereits ein Konto',
      );
    });
  });
});
