import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { makeJob, makeKpis } from '../../testing/test-data';
import { JobStore } from './job.store';
import { ToastService } from './toast.service';

const PIPELINE_JOB_KEY = 'talentmatch.pipeline.jobId';

describe('JobStore', () => {
  let store: JobStore;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.removeItem(PIPELINE_JOB_KEY);
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    store = TestBed.inject(JobStore);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    localStorage.removeItem(PIPELINE_JOB_KEY);
  });

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
    expect(localStorage.getItem(PIPELINE_JOB_KEY)).toBe('9');
  });

  it('restores the last pipeline job after signing in again', () => {
    store.select(4);
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const restored = TestBed.inject(JobStore);
    http = TestBed.inject(HttpTestingController);

    expect(restored.pipelineJobId()).toBe(4);
    restored.load();
    http.expectOne('/api/jobs').flush([makeJob({ id: 3 }), makeJob({ id: 4 })]);
    http.expectOne('/api/dashboard/kpis').flush(makeKpis());
    expect(restored.pipelineJobId()).toBe(4);
  });

  it('falls back to the newest job when the remembered job no longer exists', () => {
    store.select(99);
    store.load();
    http.expectOne('/api/jobs').flush([makeJob({ id: 3 }), makeJob({ id: 4 })]);
    http.expectOne('/api/dashboard/kpis').flush(makeKpis());
    expect(store.pipelineJobId()).toBe(3);
  });

  it('replaces an updated job and removes a deleted one', () => {
    store.load();
    http.expectOne('/api/jobs').flush([makeJob({ id: 3 }), makeJob({ id: 4 })]);
    http.expectOne('/api/dashboard/kpis').flush(makeKpis());
    store.select(4);

    store
      .update(3, {
        title: 'Neu',
        department: 'Data',
        experienceLevel: 'Mid',
        location: 'Berlin',
        description: '',
        requiredSkills: ['SQL'],
      })
      .subscribe();
    http.expectOne('/api/jobs/3').flush(makeJob({ id: 3, title: 'Neu' }));
    expect(store.jobs()[0].title).toBe('Neu');

    store.delete(4).subscribe();
    http.expectOne('/api/jobs/4').flush(null);
    expect(store.jobs().map((j) => j.id)).toEqual([3]);
    expect(store.selectedJobId()).toBeNull();
    expect(localStorage.getItem(PIPELINE_JOB_KEY)).toBeNull();
    expect(store.pipelineJobId()).toBe(3);
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
