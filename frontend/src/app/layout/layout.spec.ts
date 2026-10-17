import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AppComponent } from '../app.component';
import { AuthService } from '../core/auth/auth.service';
import { ToastService } from '../core/state/toast.service';
import { makeJob, makeKpis, makeUser } from '../testing/test-data';
import { AppHeaderComponent } from './app-header.component';
import { ShellComponent } from './shell.component';

describe('layout', () => {
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

  function signIn(role: 'HR_MANAGER' | 'APPLICANT'): void {
    TestBed.inject(AuthService).login('katrin.vogel@talentmatch.de', 'secret').subscribe();
    http.expectOne('/api/auth/login').flush({
      token: 't',
      expiresAt: new Date(Date.now() + 60_000).toISOString(),
      user: makeUser({ role }),
    });
  }

  describe('AppHeaderComponent', () => {
    it('shows navigation, role chip and user for HR managers', async () => {
      signIn('HR_MANAGER');
      const fixture = TestBed.createComponent(AppHeaderComponent);
      await fixture.whenStable();
      http.expectOne('/api/jobs').flush([makeJob({ id: 4 })]);
      http.expectOne('/api/dashboard/kpis').flush(makeKpis());
      await fixture.whenStable();

      const root = fixture.nativeElement as HTMLElement;
      const links = Array.from(root.querySelectorAll('.nav-link')) as HTMLAnchorElement[];
      expect(links.map((l) => l.textContent?.trim())).toEqual([
        'Stellen',
        'Pipeline',
        'Bewerber:innen',
      ]);
      expect(links[1].getAttribute('href')).toBe('/jobs/4/pipeline');
      expect(root.querySelector('.role-chip')?.textContent).toContain('HR-Manager');
      expect(root.querySelector('.avatar')?.textContent).toContain('KV');
    });

    it('shows the portal navigation for applicants and offers the account menu', async () => {
      signIn('APPLICANT');
      const fixture = TestBed.createComponent(AppHeaderComponent);
      await fixture.whenStable();
      const root = fixture.nativeElement as HTMLElement;
      const links = Array.from(root.querySelectorAll('.nav-link')) as HTMLAnchorElement[];
      expect(links.map((l) => l.textContent?.trim())).toEqual([
        'Offene Stellen',
        'Meine Bewerbungen',
      ]);
      expect(links[1].getAttribute('href')).toBe('/portal/applications');
      expect(root.querySelector('.brand')?.getAttribute('href')).toBe('/portal/jobs');
      expect(root.querySelector('.role-chip')?.textContent).toContain('Bewerber:in');

      (root.querySelector('.account-button') as HTMLButtonElement).click();
      await fixture.whenStable();
      expect(root.querySelector('.menu')?.textContent).toContain('Passwort ändern');
      expect(root.querySelector('.menu')?.textContent).toContain('Abmelden');
    });
  });

  describe('ShellComponent', () => {
    it('renders the header and toasts', async () => {
      TestBed.inject(ToastService).error('Etwas ging schief');
      const fixture = TestBed.createComponent(ShellComponent);
      await fixture.whenStable();
      const root = fixture.nativeElement as HTMLElement;
      expect(root.querySelector('app-header')).not.toBeNull();
      expect(root.querySelector('.toast')?.textContent).toContain('Etwas ging schief');
    });
  });

  describe('AppComponent', () => {
    it('hosts the router outlet', async () => {
      const fixture = TestBed.createComponent(AppComponent);
      await fixture.whenStable();
      expect(fixture.nativeElement.querySelector('router-outlet')).not.toBeNull();
    });
  });
});
