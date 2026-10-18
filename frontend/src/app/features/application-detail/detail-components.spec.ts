import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { Duplicate } from '../../core/models/api.models';
import { makeAnalysis, makeDetail, makeDuplicate } from '../../testing/test-data';
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
    async function render(detail = makeDetail(), duplicates: Duplicate[] = []) {
      const fixture = TestBed.createComponent(ApplicationDetailComponent);
      fixture.componentRef.setInput('id', '10');
      await fixture.whenStable();
      http.expectOne('/api/applications/10').flush(detail);
      http.expectOne('/api/applications/10/duplicates').flush(duplicates);
      await fixture.whenStable();
      return fixture;
    }

    it('shows possible duplicates and merges them via the dialog', async () => {
      const fixture = await render(makeDetail({ resumeFileName: 'cv.pdf' }), [makeDuplicate()]);
      const root = fixture.nativeElement as HTMLElement;
      const hint = root.querySelector('.duplicate-hint') as HTMLElement;
      expect(hint.textContent).toContain('Mögliches Duplikat');
      expect(hint.textContent).toContain('gleicher Name');
      expect(hint.textContent).toContain('Skillprofil_Carmen_Gregor.pdf');

      (hint.querySelector('.merge-button') as HTMLButtonElement).click();
      await fixture.whenStable();
      const dialog = root.querySelector('app-merge-dialog') as HTMLElement;
      expect((dialog.querySelector('.move-select') as HTMLSelectElement).value).toBe(
        'SKILL_PROFILE',
      );

      (dialog.querySelector('.confirm') as HTMLButtonElement).click();
      const merge = http.expectOne('/api/applications/10/merge');
      expect(merge.request.body).toEqual({
        sourceId: 20,
        documents: [{ from: 'RESUME', to: 'SKILL_PROFILE' }],
      });
      merge.flush(makeDetail({ resumeFileName: 'cv.pdf', skillProfileFileName: 'Skill.pdf' }));
      await fixture.whenStable();
      expect(root.querySelector('app-merge-dialog')).toBeNull();
      expect(root.querySelector('.duplicate-hint')).toBeNull();
      http.expectOne('/api/applications/10/analyze').flush(makeDetail());
    });

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

    it('marks skills that only the skill profile confirms', async () => {
      const fixture = await render(
        makeDetail({
          analysis: makeAnalysis({
            matchingSkills: ['Java', 'Spring Boot'],
            skillsFromSkillProfile: ['Spring Boot'],
          }),
        }),
      );
      const lines = Array.from(
        (fixture.nativeElement as HTMLElement).querySelectorAll('.pro .skill-line'),
      );
      expect(lines.map((l) => l.querySelector('.self-reported') !== null)).toEqual([false, true]);
      expect(lines[1].textContent).toContain('laut Skill-Profil');
    });

    it('asks for talent pool consent with an editable mail', async () => {
      const fixture = await render(makeDetail({ status: 'INTERVIEW' }));
      const root = fixture.nativeElement as HTMLElement;
      (root.querySelector('.talent-pool-button') as HTMLButtonElement).click();
      await fixture.whenStable();

      http.expectOne('/api/applications/10/talent-pool/preview').flush({
        recipient: 'lena@example.org',
        subject: 'Ihre Bewerbung als Senior Java Developer',
        body: 'Guten Tag Lena Hoffmann,\n\n[Antwort-Links]\n\nMit freundlichen Grüßen',
        deliveryEnabled: false,
      });
      await fixture.whenStable();
      const dialog = root.querySelector('app-talent-pool-dialog') as HTMLElement;
      expect(dialog.textContent).toContain('lena@example.org');
      expect(dialog.querySelector('.delivery-hint')).not.toBeNull();

      const subject = dialog.querySelector('#pool-subject') as HTMLInputElement;
      subject.value = 'Ihre Bewerbung bei uns';
      subject.dispatchEvent(new Event('input'));
      await fixture.whenStable();
      (dialog.querySelector('.confirm') as HTMLButtonElement).click();

      const request = http.expectOne('/api/applications/10/talent-pool');
      expect(request.request.body).toEqual({
        subject: 'Ihre Bewerbung bei uns',
        body: 'Guten Tag Lena Hoffmann,\n\n[Antwort-Links]\n\nMit freundlichen Grüßen',
      });
      request.flush(
        makeDetail({
          status: 'REJECTED',
          talentPool: {
            status: 'REQUESTED',
            requestedAt: '2026-10-18T09:00:00',
            responseDeadline: '2026-11-01T09:00:00',
            reminderSentAt: null,
            confirmedAt: null,
            expiresAt: null,
          },
        }),
      );
      await fixture.whenStable();
      expect(root.querySelector('app-talent-pool-dialog')).toBeNull();
      expect(root.querySelector('.pool-status')?.textContent).toContain('Antwort bis 01.11.2026');
      expect((root.querySelector('.talent-pool-button') as HTMLButtonElement).disabled).toBeTrue();
    });

    it('cannot ask for consent without an e-mail address', async () => {
      const detail = makeDetail();
      const fixture = await render({ ...detail, candidate: { ...detail.candidate, email: null } });
      expect(
        (fixture.nativeElement.querySelector('.talent-pool-button') as HTMLButtonElement).disabled,
      ).toBeTrue();
    });

    it('deletes the application after confirmation and returns to the pipeline', async () => {
      const fixture = await render();
      const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
      const root = fixture.nativeElement as HTMLElement;
      (root.querySelector('.delete-application') as HTMLButtonElement).click();
      await fixture.whenStable();
      expect(root.querySelector('app-confirm-dialog')?.textContent).toContain('endgültig gelöscht');

      (root.querySelector('app-confirm-dialog .confirm') as HTMLButtonElement).click();
      const request = http.expectOne('/api/applications/10');
      expect(request.request.method).toBe('DELETE');
      request.flush(null);
      expect(navigate).toHaveBeenCalledWith(['/jobs', 1, 'pipeline']);
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
      expect(fixture.nativeElement.querySelector('.doc-switch')).toBeNull();
    });

    it('switches between resume, skill profile and cover letter', async () => {
      TestBed.inject(ApplicationDetailStore).detail.set(
        makeDetail({
          resumeFileName: 'cv.pdf',
          skillProfileFileName: 'skills.pdf',
          coverLetterFileName: 'brief.pdf',
        }),
      );
      const fixture = TestBed.createComponent(ResumePanelComponent);
      await fixture.whenStable();
      const root = fixture.nativeElement as HTMLElement;
      expect(root.querySelector('h4')?.textContent).toContain('Unterlagen');
      expect(root.textContent).toContain('PDF-Vorschau (3)');

      (root.querySelectorAll('.seg-opt input')[1] as HTMLInputElement).click();
      http.expectOne('/api/applications/10/resume').flush(new Blob(['%PDF']));
      await fixture.whenStable();
      const switchOptions = root.querySelectorAll('.doc-switch .seg-opt input');
      expect(
        Array.from(root.querySelectorAll('.doc-switch .seg-opt')).map((o) => o.textContent?.trim()),
      ).toEqual(['Lebenslauf', 'Skill-Profil', 'Anschreiben']);

      (switchOptions[2] as HTMLInputElement).click();
      http.expectOne('/api/applications/10/cover-letter').flush(new Blob(['%PDF']));
      await fixture.whenStable();
      expect(root.querySelector('iframe.pdf-viewer')?.getAttribute('title')).toContain('brief.pdf');
    });
  });
});
