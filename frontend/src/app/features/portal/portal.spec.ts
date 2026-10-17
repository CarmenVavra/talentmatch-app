import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { MyApplication, OpenJob } from '../../core/models/api.models';
import { applicantStatusMeta } from '../../core/util/applicant-status';
import { makeUser } from '../../testing/test-data';
import { ApplyDialogComponent } from './apply-dialog.component';
import { MyApplicationsComponent, statusSteps } from './my-applications.component';
import { PortalJobsComponent, filterOpenJobs } from './portal-jobs.component';
import { PortalStore } from './portal.store';

function openJob(overrides: Partial<OpenJob> = {}): OpenJob {
  return {
    id: 3,
    title: 'Data Engineer',
    department: 'Data',
    experienceLevel: 'Mid',
    location: 'Hamburg · Hybrid',
    description: 'Datenpipelines mit Python und Spark.',
    requiredSkills: ['Python', 'Spark'],
    createdDate: '2026-09-21',
    applied: false,
    ...overrides,
  };
}

function myApplication(overrides: Partial<MyApplication> = {}): MyApplication {
  return {
    id: 1,
    jobId: 3,
    jobTitle: 'Data Engineer',
    department: 'Data',
    location: 'Hamburg · Hybrid',
    status: 'RECEIVED',
    submittedAt: '2026-10-08T10:15:00',
    resumeFileName: 'cv.pdf',
    ...overrides,
  };
}

function pdf(): File {
  return new File(['%PDF-1.4'], 'Lebenslauf.pdf', { type: 'application/pdf' });
}

describe('portal helpers', () => {
  it('filters open jobs by text and department', () => {
    const jobs = [
      openJob(),
      openJob({
        id: 4,
        title: 'DevOps Engineer',
        department: 'Engineering',
        requiredSkills: ['Docker'],
      }),
    ];
    expect(filterOpenJobs(jobs, 'spark', 'Alle').map((j) => j.id)).toEqual([3]);
    expect(filterOpenJobs(jobs, '', 'Engineering').map((j) => j.id)).toEqual([4]);
    expect(filterOpenJobs(jobs, 'hamburg', 'Alle').length).toBe(2);
  });

  it('maps applicant statuses to labels without internal steps', () => {
    expect(applicantStatusMeta('RECEIVED').label).toBe('Eingegangen');
    expect(applicantStatusMeta('INTERVIEW').label).toBe('Einladung zum Interview');
    expect(applicantStatusMeta('OFFER').label).toBe('Zusage');
  });

  it('builds the progress steps', () => {
    expect(statusSteps('RECEIVED').map((s) => s.state)).toEqual(['current', 'open', 'open']);
    expect(statusSteps('INTERVIEW').map((s) => s.state)).toEqual(['done', 'current', 'open']);
    expect(statusSteps('OFFER').map((s) => s.label)).toEqual([
      'Eingegangen',
      'Interview',
      'Zusage',
    ]);
    expect(statusSteps('REJECTED').map((s) => s.label)).toEqual(['Eingegangen', 'Absage']);
  });
});

describe('portal', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    TestBed.inject(AuthService).login('max@example.org', 'secret').subscribe();
    http.expectOne('/api/auth/login').flush({
      token: 't',
      expiresAt: new Date(Date.now() + 60_000).toISOString(),
      user: makeUser({ role: 'APPLICANT', email: 'max@example.org' }),
    });
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  describe('PortalStore', () => {
    it('marks the job as applied and lists the new application', () => {
      const store = TestBed.inject(PortalStore);
      store.loadJobs();
      http.expectOne('/api/applicant/jobs').flush([openJob()]);

      store.apply(3, pdf()).subscribe();
      const req = http.expectOne('/api/applicant/jobs/3/applications');
      expect(req.request.body instanceof FormData).toBeTrue();
      req.flush(myApplication());

      expect(store.jobs()[0].applied).toBeTrue();
      expect(store.applications().length).toBe(1);
    });

    it('drops cached data when another account signs in', () => {
      const store = TestBed.inject(PortalStore);
      store.loadApplications();
      http.expectOne('/api/applicant/applications').flush([myApplication()]);
      expect(store.applications().length).toBe(1);

      const auth = TestBed.inject(AuthService);
      auth.logout(false);
      auth.login('lea@example.org', 'secret').subscribe();
      http.expectOne('/api/auth/login').flush({
        token: 't2',
        expiresAt: new Date(Date.now() + 60_000).toISOString(),
        user: makeUser({ role: 'APPLICANT', email: 'lea@example.org' }),
      });
      store.loadApplications();
      expect(store.applications()).toEqual([]);
      http.expectOne('/api/applicant/applications').flush([]);
    });
  });

  describe('PortalJobsComponent', () => {
    it('lists open jobs and opens the apply dialog', async () => {
      const fixture = TestBed.createComponent(PortalJobsComponent);
      await fixture.whenStable();
      http
        .expectOne('/api/applicant/jobs')
        .flush([openJob(), openJob({ id: 4, title: 'DevOps Engineer', applied: true })]);
      await fixture.whenStable();
      const root = fixture.nativeElement as HTMLElement;

      expect(root.querySelectorAll('.tile').length).toBe(2);
      expect(root.querySelectorAll('.applied').length).toBe(1);
      expect(root.textContent).not.toContain('Score');

      (root.querySelector('.apply') as HTMLButtonElement).click();
      await fixture.whenStable();
      expect(root.querySelector('app-apply-dialog')).not.toBeNull();
    });
  });

  describe('ApplyDialogComponent', () => {
    it('rejects non-PDF files and submits a PDF', async () => {
      const fixture = TestBed.createComponent(ApplyDialogComponent);
      fixture.componentRef.setInput('job', openJob());
      await fixture.whenStable();
      const root = fixture.nativeElement as HTMLElement;
      const send = () => root.querySelector('.send') as HTMLButtonElement;
      expect(send().disabled).toBeTrue();

      const drop = (file: File) => {
        const dataTransfer = new DataTransfer();
        dataTransfer.items.add(file);
        root
          .querySelector('.dropzone')!
          .dispatchEvent(new DragEvent('drop', { dataTransfer, cancelable: true }));
      };
      drop(new File(['x'], 'foto.png'));
      await fixture.whenStable();
      expect(root.querySelector('.alert')?.textContent).toContain('nur PDF');
      expect(send().disabled).toBeTrue();

      drop(pdf());
      await fixture.whenStable();
      expect(root.querySelector('.file-name')?.textContent).toContain('Lebenslauf.pdf');
      send().click();
      await fixture.whenStable();
      expect(send().textContent).toContain('Wird gesendet');

      http.expectOne('/api/applicant/jobs/3/applications').flush(myApplication());
      await fixture.whenStable();
      expect(root.querySelector('.success')?.textContent).toContain('Vielen Dank');
    });

    it('shows the server message on conflict', async () => {
      const fixture = TestBed.createComponent(ApplyDialogComponent);
      fixture.componentRef.setInput('job', openJob());
      await fixture.whenStable();
      const root = fixture.nativeElement as HTMLElement;
      const dataTransfer = new DataTransfer();
      dataTransfer.items.add(pdf());
      root
        .querySelector('.dropzone')!
        .dispatchEvent(new DragEvent('drop', { dataTransfer, cancelable: true }));
      await fixture.whenStable();
      (root.querySelector('.send') as HTMLButtonElement).click();
      http
        .expectOne('/api/applicant/jobs/3/applications')
        .flush(
          { detail: 'Sie haben sich auf diese Stelle bereits beworben.' },
          { status: 409, statusText: 'Conflict' },
        );
      await fixture.whenStable();
      expect(root.querySelector('.alert')?.textContent).toContain('bereits beworben');
    });
  });

  describe('MyApplicationsComponent', () => {
    it('shows each application with its status but no score', async () => {
      const fixture = TestBed.createComponent(MyApplicationsComponent);
      await fixture.whenStable();
      http
        .expectOne('/api/applicant/applications')
        .flush([
          myApplication({ id: 1, status: 'INTERVIEW' }),
          myApplication({ id: 2, jobTitle: 'DevOps Engineer', status: 'RECEIVED' }),
        ]);
      await fixture.whenStable();
      const root = fixture.nativeElement as HTMLElement;

      const tags = Array.from(root.querySelectorAll('.status-tag')).map((t) =>
        t.textContent?.trim(),
      );
      expect(tags).toEqual(['Einladung zum Interview', 'Eingegangen']);
      expect(root.querySelectorAll('.step.current').length).toBe(2);
      expect(root.textContent).not.toContain('Score');
    });

    it('shows an empty state', async () => {
      const fixture = TestBed.createComponent(MyApplicationsComponent);
      await fixture.whenStable();
      http.expectOne('/api/applicant/applications').flush([]);
      await fixture.whenStable();
      expect(fixture.nativeElement.textContent).toContain(
        'Sie haben sich noch auf keine Stelle beworben.',
      );
    });
  });
});
