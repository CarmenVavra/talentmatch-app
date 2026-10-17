import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { FormControl, FormGroup } from '@angular/forms';
import { Router, provideRouter } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { makeUser } from '../../testing/test-data';
import { ChangePasswordComponent, passwordsMatch } from './change-password.component';
import { ForbiddenComponent } from './forbidden.component';
import { LoginComponent } from './login.component';

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

  describe('ForbiddenComponent', () => {
    it('explains the missing permission and offers logout', async () => {
      const auth = TestBed.inject(AuthService);
      const logout = spyOn(auth, 'logout');
      const fixture = TestBed.createComponent(ForbiddenComponent);
      await fixture.whenStable();
      expect(fixture.nativeElement.textContent).toContain('HR-Manager:innen vorbehalten');
      (fixture.nativeElement.querySelector('button') as HTMLButtonElement).click();
      expect(logout).toHaveBeenCalled();
    });
  });
});
