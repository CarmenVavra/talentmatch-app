import { HttpEventType, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { makeDetail, makeSummary } from '../../testing/test-data';
import { PipelineStore } from './pipeline.store';
import { MAX_FILE_BYTES, validateFile } from '../../core/util/file';
import { UploadStore } from './upload.store';

function pdf(name = 'Lebenslauf_Maria_Schulz.pdf', size = 1000): File {
  return new File([new Uint8Array(size)], name, { type: 'application/pdf' });
}

describe('validateFile', () => {
  it('accepts PDFs up to 10 MB', () => {
    expect(validateFile(pdf())).toBeNull();
  });

  it('rejects other file types and large files', () => {
    expect(validateFile(new File(['x'], 'foto.png'))).toContain('nur PDF');
    const big = pdf('gross.pdf');
    Object.defineProperty(big, 'size', { value: MAX_FILE_BYTES + 1 });
    expect(validateFile(big)).toContain('10 MB');
  });
});

describe('UploadStore', () => {
  let store: UploadStore;
  let pipeline: jasmine.SpyObj<PipelineStore>;
  let http: HttpTestingController;

  beforeEach(() => {
    jasmine.clock().install();
    pipeline = jasmine.createSpyObj<PipelineStore>('PipelineStore', ['upsert']);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        UploadStore,
        { provide: PipelineStore, useValue: pipeline },
      ],
    });
    store = TestBed.inject(UploadStore);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    jasmine.clock().uninstall();
    http.verify();
  });

  it('marks invalid files as non-retryable errors without uploading', () => {
    store.addFiles(1, [new File(['x'], 'foto.png')]);
    const [item] = store.uploads();
    expect(item.stage).toBe('error');
    expect(item.retryable).toBeFalse();
    http.expectNone('/api/jobs/1/applications');
  });

  it('runs upload → text extraction → analysis and inserts the card', () => {
    store.addFiles(1, [pdf()]);
    const upload = http.expectOne('/api/jobs/1/applications');
    expect(upload.request.body instanceof FormData).toBeTrue();

    upload.event({ type: HttpEventType.UploadProgress, loaded: 100, total: 100 });
    expect(store.uploads()[0].stage).toBe('uploaded');
    expect(store.uploads()[0].progress).toBe(15);

    upload.flush(makeSummary({ id: 50, status: 'NEW', overallScore: null }));
    expect(store.uploads()[0].stage).toBe('extracted');
    expect(store.uploads()[0].progress).toBe(45);
    expect(pipeline.upsert).toHaveBeenCalledTimes(1);

    jasmine.clock().tick(500);
    expect(store.uploads()[0].stage).toBe('analyzing');
    http.expectOne('/api/applications/50/analyze').flush(makeDetail({ id: 50, status: 'MATCHED' }));

    const done = store.uploads()[0];
    expect(done.stage).toBe('done');
    expect(done.progress).toBe(100);
    expect(done.score).toBe(91);
    expect(pipeline.upsert).toHaveBeenCalledTimes(2);
  });

  it('retries only the analysis when it failed after the upload', () => {
    store.addFiles(1, [pdf()]);
    http.expectOne('/api/jobs/1/applications').flush(makeSummary({ id: 51, overallScore: null }));
    jasmine.clock().tick(500);
    http
      .expectOne('/api/applications/51/analyze')
      .flush({ detail: 'Kein Key' }, { status: 502, statusText: 'Bad Gateway' });

    const failed = store.uploads()[0];
    expect(failed.stage).toBe('error');
    expect(failed.error).toBe('Analyse fehlgeschlagen – erneut versuchen');
    expect(failed.retryable).toBeTrue();

    store.retry(1, failed.id);
    http.expectNone('/api/jobs/1/applications');
    http.expectOne('/api/applications/51/analyze').flush(makeDetail({ id: 51 }));
    expect(store.uploads()[0].stage).toBe('done');
  });

  it('shows the server message when the upload itself fails', () => {
    store.addFiles(1, [pdf()]);
    http
      .expectOne('/api/jobs/1/applications')
      .flush(
        { detail: 'Im PDF wurde kein Text gefunden.' },
        { status: 400, statusText: 'Bad Request' },
      );
    expect(store.uploads()[0].error).toBe('Im PDF wurde kein Text gefunden.');
  });

  it('clears finished uploads', () => {
    store.addFiles(1, [new File(['x'], 'foto.png')]);
    store.clearFinished();
    expect(store.hasUploads()).toBeFalse();
  });
});
