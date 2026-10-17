import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { makeJob, makeKpis } from '../../testing/test-data';
import { JobStore } from './job.store';
import { ToastService } from './toast.service';

describe('JobStore', () => {
  let store: JobStore;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    store = TestBed.inject(JobStore);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads jobs and KPIs together', () => {
    store.load();
    expect(store.loading()).toBeTrue();
    http.expectOne('/api/jobs').flush([makeJob({ id: 3 }), makeJob({ id: 4 })]);
    http.expectOne('/api/dashboard/kpis').flush(makeKpis());

    expect(store.loading()).toBeFalse();
    expect(store.jobs().length).toBe(2);
    expect(store.kpis()?.openJobs).toBe(6);
    expect(store.pipelineJobId()).toBe(3);
  });

  it('persists the chosen view mode', () => {
    localStorage.removeItem('talentmatch.jobs.viewMode');
    try {
      expect(store.viewMode()).toBe('tiles');
      store.setViewMode('table');
      expect(store.viewMode()).toBe('table');
      expect(localStorage.getItem('talentmatch.jobs.viewMode')).toBe('table');
    } finally {
      localStorage.removeItem('talentmatch.jobs.viewMode');
    }
  });

  it('remembers the selected job for the pipeline link', () => {
    store.select(9);
    expect(store.pipelineJobId()).toBe(9);
  });

  it('reports load errors', () => {
    store.load();
    const jobs = http.expectOne('/api/jobs');
    const kpis = http.expectOne('/api/dashboard/kpis');
    jobs.flush({ detail: 'Kaputt' }, { status: 500, statusText: 'Error' });
    // forkJoin bricht den zweiten Request ab
    expect(kpis.cancelled).toBeTrue();
    expect(store.error()).toBe('Kaputt');
    expect(store.loading()).toBeFalse();
  });

  it('prepends created jobs and updates the KPIs', () => {
    store.kpis.set(makeKpis());
    store.jobs.set([makeJob({ id: 1 })]);
    store
      .create({
        title: 'QA Engineer',
        department: 'Engineering',
        experienceLevel: 'Mid',
        location: 'Berlin',
        description: '',
        requiredSkills: ['Cypress'],
      })
      .subscribe();
    http.expectOne('/api/jobs').flush(makeJob({ id: 7, title: 'QA Engineer' }));

    expect(store.jobs()[0].id).toBe(7);
    expect(store.kpis()?.openJobs).toBe(7);
  });
});

describe('ToastService', () => {
  beforeEach(() => jasmine.clock().install());
  afterEach(() => jasmine.clock().uninstall());

  it('shows and auto-dismisses messages', () => {
    const toasts = new ToastService();
    toasts.error('Fehler');
    toasts.success('Gespeichert');
    expect(toasts.toasts().map((t) => t.kind)).toEqual(['error', 'success']);
    jasmine.clock().tick(6000);
    expect(toasts.toasts()).toEqual([]);
  });
});
