import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { JobStore } from '../../core/state/job.store';
import { ToastService } from '../../core/state/toast.service';
import { makeAnalysis, makeDetail } from '../../testing/test-data';
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
  });

  afterEach(() => http.verify());

  it('loads the detail and remembers the job for the pipeline link', () => {
    expect(store.detail()?.candidate.lastName).toBe('Hoffmann');
    expect(store.discussedCount()).toBe(1);
    expect(TestBed.inject(JobStore).pipelineJobId()).toBe(1);
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
    store.loadResume();
    http.expectOne('/api/applications/10/resume').flush(new Blob(['%PDF']));
    expect(store.resumeUrl()).toMatch(/^blob:/);
  });
});
