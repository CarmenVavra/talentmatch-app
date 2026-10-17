import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { makeJob, makeKpis } from '../../testing/test-data';
import { JobCreateComponent } from './job-create.component';
import { JobDashboardComponent } from './job-dashboard.component';
import { filterJobs, statusSegments } from './job-filters';

const JOBS = [
  makeJob({ id: 1 }),
  makeJob({
    id: 2,
    title: 'Data Engineer',
    department: 'Data',
    requiredSkills: ['Python', 'Spark'],
    averageScore: null,
  }),
];

describe('job filters', () => {
  it('searches title, department and skills case-insensitively', () => {
    expect(filterJobs(JOBS, 'spark', 'Alle').map((j) => j.id)).toEqual([2]);
    expect(filterJobs(JOBS, 'JAVA', 'Alle').map((j) => j.id)).toEqual([1]);
    expect(filterJobs(JOBS, '', 'Data').map((j) => j.id)).toEqual([2]);
    expect(filterJobs(JOBS, 'java', 'Data')).toEqual([]);
  });

  it('builds status bar segments without empty statuses', () => {
    const segments = statusSegments(JOBS[0]);
    expect(segments.map((s) => s.key)).toEqual(['NEW', 'MATCHED', 'INTERVIEW', 'HIRED']);
    expect(segments[0].short).toBe('neu');
  });
});

describe('job pages', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  describe('JobDashboardComponent', () => {
    async function render() {
      const fixture = TestBed.createComponent(JobDashboardComponent);
      await fixture.whenStable();
      http.expectOne('/api/jobs').flush(JOBS);
      http.expectOne('/api/dashboard/kpis').flush(makeKpis());
      await fixture.whenStable();
      return fixture;
    }

    it('shows KPIs and one tile per job', async () => {
      const fixture = await render();
      const root = fixture.nativeElement as HTMLElement;
      expect(root.querySelectorAll('.kpi').length).toBe(4);
      expect(root.querySelector('.kpi-value')?.textContent).toContain('6');
      expect(root.querySelectorAll('.tile').length).toBe(2);
      expect(root.textContent).toContain('+18 seit letzter Woche');
      expect(root.querySelector('.tile .avg-badge')?.textContent?.trim()).toBe('72');
    });

    it('switches to the table view', async () => {
      const fixture = await render();
      const root = fixture.nativeElement as HTMLElement;
      const radios = root.querySelectorAll('.view-toggle input');
      (radios[1] as HTMLInputElement).click();
      await fixture.whenStable();
      expect(root.querySelectorAll('tbody tr').length).toBe(2);
    });

    it('shows the empty state when nothing matches', async () => {
      const fixture = await render();
      const root = fixture.nativeElement as HTMLElement;
      const search = root.querySelector('input[type=search]') as HTMLInputElement;
      search.value = 'cobol';
      search.dispatchEvent(new Event('input'));
      await fixture.whenStable();
      expect(root.textContent).toContain('Keine Stellen gefunden.');
    });
  });

  describe('JobCreateComponent', () => {
    it('adds skills as chips and creates the job', async () => {
      const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
      const fixture = TestBed.createComponent(JobCreateComponent);
      await fixture.whenStable();
      const root = fixture.nativeElement as HTMLElement;

      const set = (selector: string, value: string) => {
        const el = root.querySelector(selector) as HTMLInputElement;
        el.value = value;
        el.dispatchEvent(new Event('input'));
      };
      set('#title', 'QA Engineer');
      set('#location', 'Berlin · Remote');
      set('#skillInput', 'Cypress, Playwright');
      (root.querySelector('#skillInput') as HTMLInputElement).dispatchEvent(
        new KeyboardEvent('keydown', { key: 'Enter' }),
      );
      await fixture.whenStable();
      expect(root.querySelectorAll('.skill-chip').length).toBe(2);

      (root.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
      const req = http.expectOne('/api/jobs');
      expect(req.request.body).toEqual(
        jasmine.objectContaining({
          title: 'QA Engineer',
          department: 'Engineering',
          experienceLevel: 'Mid',
          requiredSkills: ['Cypress', 'Playwright'],
        }),
      );
      req.flush(makeJob({ id: 9 }));
      expect(navigate).toHaveBeenCalledWith(['/jobs', 9, 'pipeline']);
    });

    it('requires at least one skill', async () => {
      const fixture = TestBed.createComponent(JobCreateComponent);
      await fixture.whenStable();
      const root = fixture.nativeElement as HTMLElement;
      (root.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
      await fixture.whenStable();
      http.expectNone('/api/jobs');
      expect(root.textContent).toContain('Mindestens ein Skill ist erforderlich.');
    });
  });
});
