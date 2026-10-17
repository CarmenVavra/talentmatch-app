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
    localStorage.removeItem('talentmatch.jobs.viewMode');
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    localStorage.removeItem('talentmatch.jobs.viewMode');
  });

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

    it('keeps the table view after navigating away and back', async () => {
      const first = await render();
      const radios = (first.nativeElement as HTMLElement).querySelectorAll('.view-toggle input');
      (radios[1] as HTMLInputElement).click();
      await first.whenStable();
      first.destroy();

      // Neuer Komponenten-Aufbau wie bei der Rückkehr zur Route /jobs
      const second = await render();
      const root = second.nativeElement as HTMLElement;
      expect(root.querySelectorAll('tbody tr').length).toBe(2);
      expect(root.querySelectorAll('.tile').length).toBe(0);
      expect(
        (root.querySelectorAll('.view-toggle input')[1] as HTMLInputElement).checked,
      ).toBeTrue();
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

    it('makes the selected description text bold and previews it', async () => {
      const fixture = TestBed.createComponent(JobCreateComponent);
      await fixture.whenStable();
      const root = fixture.nativeElement as HTMLElement;
      const textarea = root.querySelector('#description') as HTMLTextAreaElement;
      textarea.value = 'Ihre Aufgaben: Entwicklung';
      textarea.dispatchEvent(new Event('input'));
      textarea.setSelectionRange(0, 13);

      (root.querySelector('.bold-button') as HTMLButtonElement).click();
      await fixture.whenStable();
      expect(textarea.value).toBe('**Ihre Aufgaben**: Entwicklung');
      expect(textarea.value.slice(textarea.selectionStart, textarea.selectionEnd)).toBe(
        'Ihre Aufgaben',
      );

      textarea.setSelectionRange(19, 30);
      textarea.dispatchEvent(new KeyboardEvent('keydown', { key: 'b', ctrlKey: true }));
      await fixture.whenStable();
      expect(textarea.value).toBe('**Ihre Aufgaben**: **Entwicklung**');

      const preview = Array.from(root.querySelectorAll('.seg-opt')).find((o) =>
        o.textContent?.includes('Vorschau'),
      );
      (preview?.querySelector('input') as HTMLInputElement).click();
      await fixture.whenStable();
      expect(textarea.hidden).toBeTrue();
      expect(
        Array.from(root.querySelectorAll('.description-preview strong')).map((s) => s.textContent),
      ).toEqual(['Ihre Aufgaben', 'Entwicklung']);
    });

    it('explains a too long description and does not submit', async () => {
      const fixture = TestBed.createComponent(JobCreateComponent);
      await fixture.whenStable();
      const root = fixture.nativeElement as HTMLElement;
      const set = (selector: string, value: string) => {
        const el = root.querySelector(selector) as HTMLInputElement;
        el.value = value;
        el.dispatchEvent(new Event('input'));
      };
      set('#title', 'QA Engineer');
      set('#location', 'Berlin');
      set('#skillInput', 'Cypress');
      set('#description', 'x'.repeat(20012));
      await fixture.whenStable();
      expect(root.querySelector('.char-count')?.textContent).toContain('20.012 / 20.000');
      expect(root.querySelector('#description')?.classList).toContain('is-invalid');

      (root.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
      await fixture.whenStable();
      http.expectNone('/api/jobs');
      expect(root.textContent).toContain('12 Zeichen zu lang');
    });

    it('focuses the first invalid field when submitting an incomplete form', async () => {
      const fixture = TestBed.createComponent(JobCreateComponent);
      document.body.appendChild(fixture.nativeElement);
      try {
        await fixture.whenStable();
        const root = fixture.nativeElement as HTMLElement;
        (root.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
        await fixture.whenStable();
        expect(document.activeElement).toBe(root.querySelector('#title'));
      } finally {
        fixture.nativeElement.remove();
      }
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

    describe('edit mode', () => {
      async function renderEdit(
        job = makeJob({ id: 4, department: 'Legal', totalApplications: 3 }),
      ) {
        const fixture = TestBed.createComponent(JobCreateComponent);
        fixture.componentRef.setInput('jobId', String(job.id));
        await fixture.whenStable();
        http.expectOne(`/api/jobs/${job.id}`).flush(job);
        await fixture.whenStable();
        return fixture;
      }

      it('prefills the form with the existing job', async () => {
        const fixture = await renderEdit();
        const root = fixture.nativeElement as HTMLElement;
        expect(root.querySelector('h1')?.textContent).toContain('Stelle bearbeiten');
        expect((root.querySelector('#title') as HTMLInputElement).value).toBe(
          'Senior Java Developer',
        );
        expect((root.querySelector('#description') as HTMLTextAreaElement).value).toBe(
          'Microservices',
        );
        expect(root.querySelectorAll('.skill-chip').length).toBe(3);
        // Abweichende Abteilung bleibt wählbar und ausgewählt
        const legal = Array.from(root.querySelectorAll('.seg-opt')).find((o) =>
          o.textContent?.includes('Legal'),
        );
        expect((legal?.querySelector('input') as HTMLInputElement).checked).toBeTrue();
        expect(root.querySelector('.edit-hint')?.textContent).toContain('3 Bewerbungen');
        expect(root.querySelector('.cancel')?.getAttribute('href')).toBe('/jobs/4/pipeline');
      });

      it('saves the changes with PUT and returns to the pipeline', async () => {
        const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
        const fixture = await renderEdit();
        const root = fixture.nativeElement as HTMLElement;
        const title = root.querySelector('#title') as HTMLInputElement;
        title.value = 'Lead Java Developer';
        title.dispatchEvent(new Event('input'));
        expect(root.querySelector('.submit')?.textContent).toContain('Änderungen speichern');

        (root.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
        const req = http.expectOne('/api/jobs/4');
        expect(req.request.method).toBe('PUT');
        expect(req.request.body).toEqual(
          jasmine.objectContaining({
            title: 'Lead Java Developer',
            department: 'Legal',
            experienceLevel: 'Senior',
            requiredSkills: ['Java', 'Spring Boot', 'Kubernetes'],
          }),
        );
        req.flush(makeJob({ id: 4, title: 'Lead Java Developer' }));
        expect(navigate).toHaveBeenCalledWith(['/jobs', 4, 'pipeline']);
      });

      it('shows an error when the job cannot be loaded', async () => {
        const fixture = TestBed.createComponent(JobCreateComponent);
        fixture.componentRef.setInput('jobId', '99');
        await fixture.whenStable();
        http
          .expectOne('/api/jobs/99')
          .flush(
            { detail: 'Stelle mit ID 99 wurde nicht gefunden.' },
            { status: 404, statusText: 'NF' },
          );
        await fixture.whenStable();
        const root = fixture.nativeElement as HTMLElement;
        expect(root.querySelector('.alert')?.textContent).toContain('nicht gefunden');
        expect((root.querySelector('.submit') as HTMLButtonElement).disabled).toBeTrue();
      });
    });
  });
});
