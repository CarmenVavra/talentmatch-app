import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ToastService } from '../../core/state/toast.service';
import { makeDetail, makeJob, makeSummary } from '../../testing/test-data';
import { PipelineStore } from './pipeline.store';

describe('PipelineStore', () => {
  let store: PipelineStore;
  let http: HttpTestingController;

  const apps = [
    makeSummary({ id: 1, overallScore: 61, status: 'MATCHED' }),
    makeSummary({
      id: 2,
      overallScore: null,
      status: 'NEW',
      candidate: { ...makeSummary().candidate, firstName: 'Hannah', lastName: 'Lange' },
      skills: ['Kafka'],
    }),
    makeSummary({ id: 3, overallScore: 84, status: 'MATCHED' }),
    makeSummary({ id: 4, overallScore: 39, status: 'REJECTED' }),
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), PipelineStore],
    });
    store = TestBed.inject(PipelineStore);
    http = TestBed.inject(HttpTestingController);
    store.load(1);
    http.expectOne('/api/jobs/1').flush(makeJob());
    http.expectOne('/api/jobs/1/applications').flush(apps);
  });

  afterEach(() => http.verify());

  function column(key: string): number[] {
    return store
      .columns()
      .find((c) => c.meta.key === key)!
      .cards.map((c) => c.id);
  }

  it('groups applications into status columns sorted by score', () => {
    expect(store.total()).toBe(4);
    expect(column('MATCHED')).toEqual([3, 1]);
    expect(column('NEW')).toEqual([2]);
    expect(store.columns().length).toBe(5);
  });

  it('filters by minimum score but keeps unanalysed cards visible', () => {
    store.minScore.set(75);
    expect(column('MATCHED')).toEqual([3]);
    expect(column('NEW')).toEqual([2]);
    expect(column('REJECTED')).toEqual([]);
  });

  it('filters by name or skill', () => {
    store.query.set('kafka');
    expect(column('NEW')).toEqual([2]);
    expect(column('MATCHED')).toEqual([]);
    store.query.set('weber');
    expect(column('MATCHED')).toEqual([3, 1]);
  });

  it('moves cards optimistically and persists the status', () => {
    store.moveTo(1, 'INTERVIEW');
    expect(column('INTERVIEW')).toEqual([1]);
    const req = http.expectOne('/api/applications/1');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ status: 'INTERVIEW' });
    req.flush(makeSummary({ id: 1, overallScore: 61, status: 'INTERVIEW' }));
    expect(column('INTERVIEW')).toEqual([1]);
  });

  it('rolls back a failed move and shows a message', () => {
    const toast = spyOn(TestBed.inject(ToastService), 'error');
    store.moveTo(1, 'HIRED');
    http.expectOne('/api/applications/1').flush({}, { status: 500, statusText: 'Error' });
    expect(column('HIRED')).toEqual([]);
    expect(column('MATCHED')).toEqual([3, 1]);
    expect(toast).toHaveBeenCalled();
  });

  it('runs the AI analysis and updates the card', () => {
    store.analyze(2);
    expect(store.isAnalyzing(2)).toBeTrue();
    http
      .expectOne('/api/applications/2/analyze')
      .flush(makeDetail({ id: 2, status: 'MATCHED', job: { ...makeDetail().job, id: 1 } }));
    expect(store.isAnalyzing(2)).toBeFalse();
    expect(column('NEW')).toEqual([]);
    expect(column('MATCHED')).toContain(2);
  });

  it('ignores applications of other jobs on upsert', () => {
    store.upsert(makeSummary({ id: 99, jobPositionId: 5 }));
    expect(store.total()).toBe(4);
  });
});
