import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { makeAnalysis, makeDetail } from '../../testing/test-data';
import { ApplicationDetailComponent } from './application-detail.component';
import { ApplicationDetailStore } from './application-detail.store';
import { InterviewHelperComponent } from './interview-helper.component';
import { ResumePanelComponent } from './resume-panel.component';
import { ScorePanelComponent } from './score-panel.component';

describe('ScorePanelComponent', () => {
  it('shows score, verdict, 20 segments and sub-scores', async () => {
    const fixture = TestBed.createComponent(ScorePanelComponent);
    fixture.componentRef.setInput('analysis', makeAnalysis());
    await fixture.whenStable();
    const root = fixture.nativeElement as HTMLElement;
    expect(root.querySelector('.score-number')?.textContent).toContain('91');
    expect(root.querySelector('.verdict')?.textContent).toContain('Starke Passung');
    expect(root.querySelectorAll('.cells > div').length).toBe(20);
    expect(root.querySelectorAll('.sub').length).toBe(4);
  });

  it('shows the pending state without analysis', async () => {
    const fixture = TestBed.createComponent(ScorePanelComponent);
    await fixture.whenStable();
    const root = fixture.nativeElement as HTMLElement;
    expect(root.querySelector('.score-number')?.textContent).toContain('—');
    expect(root.querySelector('.verdict')?.textContent).toContain('Ausstehend');
    expect(root.textContent).toContain('Noch kein Score');
  });
});

describe('detail components', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        ApplicationDetailStore,
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  describe('ApplicationDetailComponent', () => {
    async function render(detail = makeDetail()) {
      const fixture = TestBed.createComponent(ApplicationDetailComponent);
      fixture.componentRef.setInput('id', '10');
      await fixture.whenStable();
      http.expectOne('/api/applications/10').flush(detail);
      await fixture.whenStable();
      return fixture;
    }

    it('renders header, AI summary and pro/contra matrix', async () => {
      const fixture = await render();
      const root = fixture.nativeElement as HTMLElement;
      expect(root.querySelector('.name')?.textContent).toContain('Lena Hoffmann');
      expect(root.querySelector('.status-tag')?.textContent).toContain('Interview');
      expect(root.querySelector('.summary-text')?.textContent).toContain('Sehr starke Passung.');
      expect(root.textContent).toContain('2 von 3 Anforderungen erfüllt');
      expect(root.querySelector('.risks')?.textContent).toContain('Kündigungsfrist');
      expect((root.querySelector('.invite') as HTMLButtonElement).disabled).toBeTrue();
    });

    it('changes the status via the action buttons', async () => {
      const fixture = await render();
      const buttons = fixture.nativeElement.querySelectorAll('.actions button');
      (buttons[1] as HTMLButtonElement).click();
      const req = http.expectOne('/api/applications/10');
      expect(req.request.body).toEqual({ status: 'HIRED' });
      req.flush({});
    });

    it('shows the pending panel and starts the analysis', async () => {
      const fixture = await render(makeDetail({ analysis: null }));
      const root = fixture.nativeElement as HTMLElement;
      expect(root.textContent).toContain('KI-Analyse ausstehend');
      (root.querySelector('.analyze') as HTMLButtonElement).click();
      await fixture.whenStable();
      expect(root.querySelector('.analyze')?.textContent).toContain('Analyse läuft');
      http.expectOne('/api/applications/10/analyze').flush(makeDetail());
      await fixture.whenStable();
      expect(root.textContent).toContain('KI-Zusammenfassung');
    });
  });

  describe('InterviewHelperComponent', () => {
    it('shows progress and toggles questions', async () => {
      const store = TestBed.inject(ApplicationDetailStore);
      store.detail.set(makeDetail());
      const fixture = TestBed.createComponent(InterviewHelperComponent);
      await fixture.whenStable();
      const root = fixture.nativeElement as HTMLElement;
      expect(root.textContent).toContain('1 / 2 besprochen');

      (root.querySelectorAll('.question')[1] as HTMLButtonElement).click();
      await fixture.whenStable();
      expect(root.textContent).toContain('2 / 2 besprochen');
      http.expectOne('/api/applications/10/questions/1').flush(null);
    });
  });

  describe('ResumePanelComponent', () => {
    it('shows the extracted career and a generated preview without original file', async () => {
      TestBed.inject(ApplicationDetailStore).detail.set(makeDetail());
      const fixture = TestBed.createComponent(ResumePanelComponent);
      await fixture.whenStable();
      const root = fixture.nativeElement as HTMLElement;
      expect(root.querySelector('.entry-role')?.textContent).toContain('Senior Backend Engineer');
      expect(root.querySelectorAll('.tags .tag').length).toBe(3);

      (root.querySelectorAll('.seg-opt input')[1] as HTMLInputElement).click();
      await fixture.whenStable();
      expect(root.querySelector('.page-name')?.textContent).toContain('Lena Hoffmann');
      expect(root.textContent).toContain('Generierte Vorschau');
    });

    it('loads the original PDF when available', async () => {
      TestBed.inject(ApplicationDetailStore).detail.set(makeDetail({ resumeFileName: 'cv.pdf' }));
      const fixture = TestBed.createComponent(ResumePanelComponent);
      await fixture.whenStable();
      (fixture.nativeElement.querySelectorAll('.seg-opt input')[1] as HTMLInputElement).click();
      http.expectOne('/api/applications/10/resume').flush(new Blob(['%PDF']));
      await fixture.whenStable();
      expect(fixture.nativeElement.querySelector('iframe.pdf-viewer')).not.toBeNull();
    });
  });
});
