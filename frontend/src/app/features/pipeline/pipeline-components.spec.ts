import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { makeJob, makeSummary } from '../../testing/test-data';
import { CandidateCardComponent } from './candidate-card.component';
import { PipelineBoardComponent } from './pipeline-board.component';
import { PipelineStore } from './pipeline.store';
import { UploadDialogComponent } from './upload-dialog.component';
import { UploadStore } from './upload.store';

describe('CandidateCardComponent', () => {
  it('shows score, name, position and up to three skills', async () => {
    const fixture = TestBed.createComponent(CandidateCardComponent);
    fixture.componentRef.setInput('application', makeSummary());
    await fixture.whenStable();
    const root = fixture.nativeElement as HTMLElement;
    expect(root.querySelector('.score')?.textContent?.trim()).toBe('84');
    expect(root.querySelector('.name')?.textContent).toContain('Jonas Weber');
    expect(root.querySelector('.role')?.textContent).toContain('Java Developer · Siemens');
    expect(root.querySelectorAll('.chip').length).toBe(3);
    expect(root.querySelector('.analyze')).toBeNull();
    expect(root.querySelector('.new-badge')).toBeNull();
  });

  it('marks new portal applications', async () => {
    const fixture = TestBed.createComponent(CandidateCardComponent);
    fixture.componentRef.setInput('application', makeSummary({ unread: true }));
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.new-badge')?.textContent).toContain('Neu');
  });

  it('offers the AI analysis for unanalysed applications without bubbling the click', async () => {
    const fixture = TestBed.createComponent(CandidateCardComponent);
    fixture.componentRef.setInput('application', makeSummary({ overallScore: null }));
    await fixture.whenStable();
    let emitted: number | undefined;
    fixture.componentInstance.analyze.subscribe((id) => (emitted = id));
    const hostClick = jasmine.createSpy('hostClick');
    (fixture.nativeElement as HTMLElement).addEventListener('click', hostClick);

    const button = fixture.nativeElement.querySelector('.analyze') as HTMLButtonElement;
    expect(fixture.nativeElement.querySelector('.score').textContent.trim()).toBe('—');
    button.click();
    expect(emitted).toBe(10);
    expect(hostClick).not.toHaveBeenCalled();

    fixture.componentRef.setInput('analyzing', true);
    await fixture.whenStable();
    expect(button.textContent).toContain('Analyse läuft');
    expect(button.disabled).toBeTrue();
  });
});

describe('PipelineBoardComponent', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function render() {
    const fixture = TestBed.createComponent(PipelineBoardComponent);
    fixture.componentRef.setInput('jobId', '1');
    await fixture.whenStable();
    http.expectOne('/api/jobs/1').flush(makeJob());
    http
      .expectOne('/api/jobs/1/applications')
      .flush([
        makeSummary({ id: 1, status: 'NEW', overallScore: null }),
        makeSummary({ id: 2, status: 'MATCHED' }),
      ]);
    await fixture.whenStable();
    return fixture;
  }

  it('renders the five status columns with their cards', async () => {
    const fixture = await render();
    const root = fixture.nativeElement as HTMLElement;
    expect(root.querySelector('.title')?.textContent).toContain('Senior Java Developer');
    const labels = Array.from(root.querySelectorAll('.column-label')).map((e) => e.textContent);
    expect(labels).toEqual(['Eingegangen', 'KI-analysiert', 'Interview', 'Zusage', 'Absage']);
    expect(root.querySelectorAll('app-candidate-card').length).toBe(2);
    expect(root.querySelectorAll('.empty').length).toBe(3);
  });

  it('opens the detail page on card click', async () => {
    const fixture = await render();
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    (fixture.nativeElement.querySelector('app-candidate-card') as HTMLElement).click();
    expect(navigate).toHaveBeenCalledWith(['/applications', 1]);
  });

  it('opens the upload dialog', async () => {
    const fixture = await render();
    (fixture.nativeElement.querySelector('.upload-button') as HTMLButtonElement).click();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('app-upload-dialog')).not.toBeNull();
  });
});

describe('UploadDialogComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), PipelineStore, UploadStore],
    });
  });

  it('lists dropped files with their stage and closes via the button', async () => {
    const fixture = TestBed.createComponent(UploadDialogComponent);
    fixture.componentRef.setInput('job', makeJob());
    await fixture.whenStable();
    let closed = false;
    fixture.componentInstance.closed.subscribe(() => (closed = true));

    const dataTransfer = new DataTransfer();
    dataTransfer.items.add(new File(['x'], 'foto.png'));
    const zone = fixture.nativeElement.querySelector('.dropzone') as HTMLElement;
    zone.dispatchEvent(new DragEvent('drop', { dataTransfer, cancelable: true }));
    await fixture.whenStable();

    const root = fixture.nativeElement as HTMLElement;
    expect(root.querySelector('.kicker')?.textContent).toContain('Senior Java Developer');
    expect(root.querySelector('.file-name')?.textContent).toContain('foto.png');
    expect(root.querySelector('.stage')?.textContent).toContain('nur PDF');
    expect(root.querySelector('.retry')).toBeNull();

    (root.querySelector('.to-pipeline') as HTMLButtonElement).click();
    expect(closed).toBeTrue();
  });
});
