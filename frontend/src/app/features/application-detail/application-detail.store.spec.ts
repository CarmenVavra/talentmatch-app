import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { JobStore } from '../../core/state/job.store';
import { ToastService } from '../../core/state/toast.service';
import { makeAnalysis, makeDetail, makeDuplicate } from '../../testing/test-data';
import { ApplicationDetailStore } from './application-detail.store';

describe('ApplicationDetailStore', () => {
  let store: ApplicationDetailStore;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), ApplicationDetailStore],
    });
    store = TestBed.inject(ApplicationDetailStore);
    http = TestBed.inject(HttpTestingController);
    store.load(10);
    http.expectOne('/api/applications/10').flush(makeDetail());
    http.expectOne('/api/applications/10/duplicates').flush([]);
  });

  afterEach(() => http.verify());

  it('loads the detail and remembers the job for the pipeline link', () => {
    expect(store.detail()?.candidate.lastName).toBe('Hoffmann');
    expect(store.discussedCount()).toBe(1);
    expect(TestBed.inject(JobStore).pipelineJobId()).toBe(1);
  });

  it('marks a new portal application as seen when it is opened', () => {
    store.load(11);
    http.expectOne('/api/applications/11').flush(makeDetail({ id: 11, unread: true }));
    const seen = http.expectOne('/api/applications/11/seen');
    expect(seen.request.method).toBe('POST');
    seen.flush(null);
    http.expectOne('/api/applications/11/duplicates').flush([]);
    expect(store.detail()?.unread).toBeFalse();
  });

  it('does not mark already seen applications again', () => {
    store.load(12);
    http.expectOne('/api/applications/12').flush(makeDetail({ id: 12 }));
    http.expectOne('/api/applications/12/duplicates').flush([]);
    http.expectNone('/api/applications/12/seen');
  });

  it('loads possible duplicates of the application', () => {
    store.load(13);
    http.expectOne('/api/applications/13').flush(makeDetail({ id: 13 }));
    http.expectOne('/api/applications/13/duplicates').flush([makeDuplicate()]);
    expect(store.duplicates().map((d) => d.id)).toEqual([20]);
  });

  it('merges a duplicate, removes the hint and restarts the analysis', () => {
    store.duplicates.set([makeDuplicate()]);
    let result: boolean | null = null;
    store.merge(
      { sourceId: 20, documents: [{ from: 'RESUME', to: 'SKILL_PROFILE' }] },
      (success) => (result = success),
    );
    expect(store.merging()).toBeTrue();
    const merge = http.expectOne('/api/applications/10/merge');
    expect(merge.request.body).toEqual({
      sourceId: 20,
      documents: [{ from: 'RESUME', to: 'SKILL_PROFILE' }],
    });
    merge.flush(makeDetail({ skillProfileFileName: 'Skillprofil.pdf', analysis: null }));

    expect(result).toBeTrue();
    expect(store.duplicates()).toEqual([]);
    expect(store.detail()?.skillProfileFileName).toBe('Skillprofil.pdf');
    expect(store.analyzing()).toBeTrue();
    http.expectOne('/api/applications/10/analyze').flush(makeDetail());
  });

  it('reports a failed merge', () => {
    let result: boolean | null = null;
    store.merge({ sourceId: 20, documents: [] }, (success) => (result = success));
    http
      .expectOne('/api/applications/10/merge')
      .flush(
        { detail: 'Nur Bewerbungen derselben Stelle.' },
        { status: 409, statusText: 'Conflict' },
      );
    expect(result).toBeFalse();
    expect(store.merging()).toBeFalse();
    http.expectNone('/api/applications/10/analyze');
  });

  it('changes the status optimistically', () => {
    store.setStatus('HIRED');
    expect(store.detail()?.status).toBe('HIRED');
    http.expectOne('/api/applications/10').flush({});
  });

  it('rolls back a failed status change', () => {
    const toast = spyOn(TestBed.inject(ToastService), 'error');
    store.setStatus('REJECTED');
    http.expectOne('/api/applications/10').flush({}, { status: 500, statusText: 'Error' });
    expect(store.detail()?.status).toBe('INTERVIEW');
    expect(toast).toHaveBeenCalled();
  });

  it('toggles interview questions and persists them', () => {
    store.toggleQuestion(1);
    expect(store.discussedCount()).toBe(2);
    const req = http.expectOne('/api/applications/10/questions/1');
    expect(req.request.body).toEqual({ discussed: true });
    req.flush(null);
  });

  it('reverts a question toggle on error', () => {
    store.toggleQuestion(0);
    http
      .expectOne('/api/applications/10/questions/0')
      .flush({}, { status: 500, statusText: 'Error' });
    expect(store.questions()[0].discussed).toBeTrue();
  });

  it('saves notes debounced', () => {
    jasmine.clock().install();
    // debounceTime vergleicht mit Date.now() – daher auch die Uhrzeit mocken
    jasmine.clock().mockDate(new Date());
    try {
      store.updateNotes('Erster');
      store.updateNotes('Erster Eindruck sehr gut');
      jasmine.clock().tick(800);
      const req = http.expectOne('/api/applications/10/notes');
      expect(req.request.body).toEqual({ notes: 'Erster Eindruck sehr gut' });
      req.flush(null);
      expect(store.notesState()).toBe('saved');
    } finally {
      jasmine.clock().uninstall();
    }
  });

  it('reports a failed analysis', () => {
    store.detail.set(makeDetail({ analysis: null }));
    store.analyze();
    expect(store.analyzing()).toBeTrue();
    http
      .expectOne('/api/applications/10/analyze')
      .flush({ detail: 'Kein OpenAI-API-Key' }, { status: 502, statusText: 'Bad Gateway' });
    expect(store.analyzing()).toBeFalse();
    expect(store.analyzeError()).toContain('Analyse fehlgeschlagen – erneut versuchen');
  });

  it('appends generated questions', () => {
    store.generateMoreQuestions();
    const extended = makeAnalysis({
      questions: [
        ...makeAnalysis().questions,
        { index: 2, topic: 'Werdegang', text: 'Neue Frage?', discussed: false },
      ],
    });
    http
      .expectOne('/api/applications/10/questions/generate')
      .flush(makeDetail({ analysis: extended }));
    expect(store.questions().length).toBe(3);
    expect(store.generating()).toBeFalse();
  });

  it('loads the original PDF as blob URL', () => {
    store.detail.set(makeDetail({ resumeFileName: 'cv.pdf' }));
    store.loadDocument('resume');
    http.expectOne('/api/applications/10/resume').flush(new Blob(['%PDF']));
    expect(store.documentUrls().resume).toMatch(/^blob:/);
  });

  it('loads optional documents once and skips missing ones', () => {
    store.detail.set(makeDetail({ skillProfileFileName: 'skills.pdf' }));
    store.loadDocument('cover-letter');
    http.expectNone('/api/applications/10/cover-letter');

    store.loadDocument('skill-profile');
    store.loadDocument('skill-profile');
    http.expectOne('/api/applications/10/skill-profile').flush(new Blob(['%PDF']));
    expect(store.documentUrls()['skill-profile']).toMatch(/^blob:/);
    store.loadDocument('skill-profile');
    http.expectNone('/api/applications/10/skill-profile');
  });

  it('keeps document errors per document', () => {
    store.detail.set(makeDetail({ coverLetterFileName: 'brief.pdf' }));
    store.loadDocument('cover-letter');
    http
      .expectOne('/api/applications/10/cover-letter')
      .flush(new Blob(['{}']), { status: 404, statusText: 'Not Found' });
    expect(store.documentErrors()['cover-letter']).toBe('Das PDF konnte nicht geladen werden.');
    expect(store.documentErrors().resume).toBeUndefined();
  });
});
