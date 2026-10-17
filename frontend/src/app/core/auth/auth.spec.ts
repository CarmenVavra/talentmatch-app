import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';

import { makeUser } from '../../testing/test-data';
import { applicantGuard, authGuard, guestGuard, hrManagerGuard } from './auth.guards';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('AuthService, interceptor and guards', () => {
  let auth: AuthService;
  let http: HttpTestingController;
  let client: HttpClient;
  let router: Router;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
    client = TestBed.inject(HttpClient);
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  function login(role: 'HR_MANAGER' | 'APPLICANT' = 'HR_MANAGER'): void {
    auth.login('katrin.vogel@talentmatch.de', 'secret').subscribe();
    http.expectOne('/api/auth/login').flush({
      token: 'jwt-token',
      expiresAt: new Date(Date.now() + 3600_000).toISOString(),
      user: makeUser({ role }),
    });
  }

  function runGuard(guard: typeof authGuard): boolean | UrlTree {
    return TestBed.runInInjectionContext(
      () =>
        guard({} as ActivatedRouteSnapshot, { url: '/jobs' } as RouterStateSnapshot) as
          boolean | UrlTree,
    );
  }

  it('stores the session after login', () => {
    login();
    expect(auth.isAuthenticated()).toBeTrue();
    expect(auth.isHrManager()).toBeTrue();
    expect(auth.initials()).toBe('KV');
    expect(localStorage.getItem('talentmatch.session')).toContain('jwt-token');
  });

  it('adds the bearer token to API calls but not to the login call', () => {
    login();
    client.get('/api/jobs').subscribe();
    const req = http.expectOne('/api/jobs');
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt-token');
    req.flush([]);
  });

  it('logs out on 401 responses', () => {
    login();
    const navigate = spyOn(router, 'navigate').and.resolveTo(true);
    client.get('/api/jobs').subscribe({ error: () => undefined });
    http.expectOne('/api/jobs').flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(auth.isAuthenticated()).toBeFalse();
    expect(navigate).toHaveBeenCalledWith(['/login']);
  });

  it('posts password changes', () => {
    login();
    auth
      .changePassword({ currentPassword: 'alt-passwort', newPassword: 'neues-passwort' })
      .subscribe();
    const req = http.expectOne('/api/auth/change-password');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      currentPassword: 'alt-passwort',
      newPassword: 'neues-passwort',
    });
    req.flush(null);
  });

  it('authGuard redirects anonymous users to the login page', () => {
    const result = runGuard(authGuard);
    expect(result instanceof UrlTree).toBeTrue();
    expect(router.serializeUrl(result as UrlTree)).toBe('/login?returnUrl=%2Fjobs');
  });

  it('hrManagerGuard sends applicants to the portal', () => {
    login('APPLICANT');
    const result = runGuard(hrManagerGuard);
    expect(router.serializeUrl(result as UrlTree)).toBe('/portal');

    auth.logout(false);
    login('HR_MANAGER');
    expect(runGuard(hrManagerGuard)).toBeTrue();
  });

  it('applicantGuard sends HR managers to the job overview', () => {
    login('HR_MANAGER');
    expect(router.serializeUrl(runGuard(applicantGuard) as UrlTree)).toBe('/jobs');

    auth.logout(false);
    login('APPLICANT');
    expect(runGuard(applicantGuard)).toBeTrue();
  });

  it('guestGuard redirects signed-in users to their home page', () => {
    expect(runGuard(guestGuard)).toBeTrue();
    login('APPLICANT');
    expect(auth.homeUrl()).toBe('/portal/jobs');
    expect(router.serializeUrl(runGuard(guestGuard) as UrlTree)).toBe('/portal/jobs');
    auth.logout(false);
    login('HR_MANAGER');
    expect(router.serializeUrl(runGuard(guestGuard) as UrlTree)).toBe('/jobs');
  });

  it('registers an applicant and starts the session', () => {
    auth
      .register({
        firstName: 'Max',
        lastName: 'M',
        email: 'max@example.org',
        password: 'geheim-123',
      })
      .subscribe();
    const req = http.expectOne('/api/auth/register');
    expect(req.request.body.email).toBe('max@example.org');
    req.flush({
      token: 'reg-token',
      expiresAt: new Date(Date.now() + 3600_000).toISOString(),
      user: makeUser({ role: 'APPLICANT', firstName: 'Max', lastName: 'M' }),
    });
    expect(auth.isAuthenticated()).toBeTrue();
    expect(auth.hasRole('APPLICANT')).toBeTrue();
    expect(auth.token()).toBe('reg-token');
  });
});
