import { HttpEventType, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { makeDetail, makeSummary } from '../../testing/test-data';
import { PipelineStore } from './pipeline.store';
import { MAX_FILE_BYTES, validateFile } from '../../core/util/file';
import { DocumentType } from '../../core/models/api.models';
import { UploadStore, groupLabel, groupProblem } from './upload.store';

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

  /** Vorabprüfung beantworten: je Datei Art/Name/E-Mail, Gruppen wie angegeben. */
  function inspect(
    files: { type: DocumentType; first?: string; last?: string; email?: string; error?: string }[],
    groups: number[][],
    needsReview = false,
  ): void {
    http.expectOne('/api/jobs/1/applications/inspect').flush({
      files: files.map((f, index) => ({
        index,
        fileName: `datei${index}.pdf`,
        type: f.error ? null : f.type,
        firstName: f.first ?? null,
        lastName: f.last ?? null,
        email: f.email ?? null,
        phone: null,
        error: f.error ?? null,
      })),
      groups: groups.map((fileIndexes) => ({
        fileIndexes,
        match: fileIndexes.length > 1 ? 'EMAIL' : 'NONE',
        needsReview,
      })),
    });
  }

  it('groups the documents of one person in a preview before uploading', () => {
    store.addFiles(1, [
      pdf('Lebenslauf_Carmen_Gregor.pdf'),
      pdf('Skillprofil_Carmen_Gregor.pdf'),
      pdf('CV_Max.pdf'),
    ]);
    expect(store.inspecting()).toBeTrue();
    inspect(
      [
        { type: 'RESUME', first: 'Carmen', last: 'Gregor', email: 'c@example.org' },
        { type: 'SKILL_PROFILE', first: 'Carmen', last: 'Gregor', email: 'c@example.org' },
        { type: 'RESUME', first: 'Max', last: 'Muster' },
      ],
      [[0, 1], [2]],
    );

    expect(store.inspecting()).toBeFalse();
    expect(store.groups().length).toBe(2);
    expect(store.draftFileCount()).toBe(3);
    expect(groupLabel(store.groups()[0])).toBe('Carmen Gregor');
    expect(store.canSubmit()).toBeTrue();
    http.expectNone('/api/jobs/1/applications');
  });

  it('lets HR fix types and assignments and blocks groups without a resume', () => {
    store.addFiles(1, [pdf('a.pdf'), pdf('b.pdf')]);
    inspect([{ type: 'RESUME' }, { type: 'RESUME' }], [[0, 1]], true);
    const [group] = store.groups();
    expect(groupProblem(group)).toContain('mehrfach');
    expect(store.canSubmit()).toBeFalse();

    store.setType(group.files[1].key, 'SKILL_PROFILE');
    expect(groupProblem(store.groups()[0])).toBeNull();

    store.moveFile(group.files[0].key, null);
    expect(store.groups().length).toBe(2);
    expect(groupProblem(store.groups()[0])).toContain('Lebenslauf fehlt');
    expect(store.canSubmit()).toBeFalse();

    store.moveFile(store.groups()[1].files[0].key, store.groups()[0].id);
    expect(store.groups().length).toBe(1);
    expect(store.canSubmit()).toBeTrue();

    store.removeFile(store.groups()[0].files[0].key);
    store.discardDraft();
    expect(store.hasDraft()).toBeFalse();
  });

  it('reports unusable files from the check as errors', () => {
    store.addFiles(1, [pdf('ok.pdf'), pdf('scan.pdf')]);
    inspect(
      [{ type: 'RESUME' }, { type: 'RESUME', error: 'Im PDF wurde kein Text gefunden.' }],
      [[0]],
    );
    expect(store.groups().length).toBe(1);
    expect(store.uploads()[0].error).toContain('kein Text');
    expect(store.uploads()[0].retryable).toBeFalse();
  });

  it('uploads one application per person with all documents and analyses it', () => {
    store.addFiles(1, [pdf('cv.pdf'), pdf('skills.pdf')]);
    inspect(
      [
        { type: 'RESUME', first: 'Carmen', last: 'Gregor' },
        { type: 'SKILL_PROFILE', first: 'Carmen', last: 'Gregor' },
      ],
      [[0, 1]],
    );
    store.submit(1);
    expect(store.hasDraft()).toBeFalse();

    const upload = http.expectOne('/api/jobs/1/applications');
    const body = upload.request.body as FormData;
    expect((body.get('file') as File).name).toBe('cv.pdf');
    expect((body.get('skillProfile') as File).name).toBe('skills.pdf');
    expect(body.has('coverLetter')).toBeFalse();
    expect(store.uploads()[0].label).toBe('Carmen Gregor');
    expect(store.uploads()[0].fileNames).toEqual(['cv.pdf', 'skills.pdf']);

    upload.event({ type: HttpEventType.UploadProgress, loaded: 100, total: 100 });
    expect(store.uploads()[0].stage).toBe('uploaded');
    upload.flush(makeSummary({ id: 50, status: 'NEW', overallScore: null }));
    expect(store.uploads()[0].stage).toBe('extracted');
    expect(pipeline.upsert).toHaveBeenCalledTimes(1);

    jasmine.clock().tick(500);
    expect(store.uploads()[0].stage).toBe('analyzing');
    http.expectOne('/api/applications/50/analyze').flush(makeDetail({ id: 50, status: 'MATCHED' }));
    expect(store.uploads()[0].stage).toBe('done');
    expect(store.uploads()[0].score).toBe(91);
    expect(pipeline.upsert).toHaveBeenCalledTimes(2);
  });

  it('retries only the analysis when it failed after the upload', () => {
    store.addFiles(1, [pdf()]);
    inspect([{ type: 'RESUME' }], [[0]]);
    store.submit(1);
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
    inspect([{ type: 'RESUME' }], [[0]]);
    store.submit(1);
    http
      .expectOne('/api/jobs/1/applications')
      .flush(
        { detail: 'Im PDF wurde kein Text gefunden.' },
        { status: 400, statusText: 'Bad Request' },
      );
    expect(store.uploads()[0].error).toBe('Im PDF wurde kein Text gefunden.');
  });

  it('shows an error when the check fails', () => {
    store.addFiles(1, [pdf()]);
    http
      .expectOne('/api/jobs/1/applications/inspect')
      .flush({ detail: 'Bitte höchstens 30 Dateien.' }, { status: 400, statusText: 'Bad Request' });
    expect(store.inspectError()).toContain('30 Dateien');
    expect(store.inspecting()).toBeFalse();
  });

  it('clears finished uploads', () => {
    store.addFiles(1, [new File(['x'], 'foto.png')]);
    store.clearFinished();
    expect(store.hasUploads()).toBeFalse();
  });
});
