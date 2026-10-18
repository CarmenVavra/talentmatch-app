import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { PublicTalentPool, TalentPoolEntry, TalentPoolInfo } from '../../core/models/api.models';
import { linkify, talentPoolDetail, talentPoolLabel } from '../../core/util/talent-pool';
import { OutboxComponent } from '../outbox/outbox.component';
import { TalentPoolAnswerComponent } from './talent-pool-answer.component';
import { TalentPoolComponent } from './talent-pool.component';

function info(overrides: Partial<TalentPoolInfo> = {}): TalentPoolInfo {
  return {
    status: 'REQUESTED',
    requestedAt: '2026-10-18T09:00:00',
    responseDeadline: '2026-11-01T09:00:00',
    reminderSentAt: null,
    confirmedAt: null,
    expiresAt: null,
    ...overrides,
  };
}

function publicView(overrides: Partial<PublicTalentPool> = {}): PublicTalentPool {
  return {
    firstName: 'Lina',
    jobTitle: 'Fullstack-Entwickler:in',
    status: 'REQUESTED',
    responseDeadline: '2026-11-01T09:00:00',
    expiresAt: null,
    ...overrides,
  };
}

describe('talent pool helpers', () => {
  it('labels and describes the talent pool state', () => {
    expect(talentPoolLabel('REQUESTED')).toBe('Evidenz angefragt');
    expect(talentPoolLabel('CONFIRMED')).toBe('In Evidenz');
    expect(talentPoolDetail(info())).toBe('angefragt am 18.10.2026 · Antwort bis 01.11.2026');
    expect(talentPoolDetail(info({ reminderSentAt: '2026-10-25T09:00:00' }))).toContain(
      'Erinnerung am 25.10.2026',
    );
    expect(
      talentPoolDetail(
        info({
          status: 'CONFIRMED',
          confirmedAt: '2026-10-20T09:00:00',
          expiresAt: '2028-10-19T09:00:00',
        }),
      ),
    ).toBe('zugestimmt am 20.10.2026 · gilt bis 19.10.2028');
  });

  it('splits mail text into text and links', () => {
    expect(linkify('Ja:\nhttp://localhost:4200/evidenz/abc?antwort=ja\nDanke')).toEqual([
      { text: 'Ja:\n', href: null },
      {
        text: 'http://localhost:4200/evidenz/abc?antwort=ja',
        href: 'http://localhost:4200/evidenz/abc?antwort=ja',
      },
      { text: '\nDanke', href: null },
    ]);
    expect(linkify('javascript:alert(1)')).toEqual([{ text: 'javascript:alert(1)', href: null }]);
  });
});

describe('talent pool pages', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  describe('TalentPoolAnswerComponent', () => {
    async function render(antwort?: string) {
      const fixture = TestBed.createComponent(TalentPoolAnswerComponent);
      fixture.componentRef.setInput('token', 'abc');
      fixture.componentRef.setInput('antwort', antwort);
      await fixture.whenStable();
      return { fixture, root: fixture.nativeElement as HTMLElement };
    }

    it('asks for consent and confirms on click only', async () => {
      const { fixture, root } = await render('ja');
      http.expectOne('/api/public/talent-pool/abc').flush(publicView());
      await fixture.whenStable();

      expect(root.querySelector('h2')?.textContent).toContain('Guten Tag Lina');
      expect(root.textContent).toContain('01.11.2026');
      expect(root.querySelector('.confirm')?.classList).toContain('btn-primary');
      http.expectNone('/api/public/talent-pool/abc/confirm');

      (root.querySelector('.confirm') as HTMLButtonElement).click();
      http
        .expectOne('/api/public/talent-pool/abc/confirm')
        .flush(publicView({ status: 'CONFIRMED', expiresAt: '2028-10-19T09:00:00' }));
      await fixture.whenStable();
      expect(root.querySelector('.success')?.textContent).toContain('Vielen Dank');
      expect(root.querySelector('.success')?.textContent).toContain('19.10.2028');
    });

    it('highlights „Nein, danke“ from the mail link and deletes on click', async () => {
      const { fixture, root } = await render('nein');
      http.expectOne('/api/public/talent-pool/abc').flush(publicView());
      await fixture.whenStable();
      expect(root.querySelector('.decline')?.classList).toContain('btn-primary');

      (root.querySelector('.decline') as HTMLButtonElement).click();
      http.expectOne('/api/public/talent-pool/abc/decline').flush(null);
      await fixture.whenStable();
      expect(root.querySelector('h2')?.textContent).toContain('gelöscht');
    });

    it('lets confirmed applicants withdraw their consent', async () => {
      const { fixture, root } = await render();
      http
        .expectOne('/api/public/talent-pool/abc')
        .flush(publicView({ status: 'CONFIRMED', expiresAt: '2028-10-19T09:00:00' }));
      await fixture.whenStable();

      (root.querySelector('.withdraw') as HTMLButtonElement).click();
      await fixture.whenStable();
      (root.querySelector('.withdraw-confirm') as HTMLButtonElement).click();
      http.expectOne('/api/public/talent-pool/abc/decline').flush(null);
      await fixture.whenStable();
      expect(root.querySelector('h2')?.textContent).toContain('gelöscht');
    });

    it('explains invalid links', async () => {
      const { fixture, root } = await render();
      http
        .expectOne('/api/public/talent-pool/abc')
        .flush({ detail: 'nicht gefunden' }, { status: 404, statusText: 'Not Found' });
      await fixture.whenStable();
      expect(root.querySelector('h2')?.textContent).toContain('nicht mehr gültig');
    });
  });

  describe('TalentPoolComponent', () => {
    it('lists confirmed and requested entries across jobs', async () => {
      const fixture = TestBed.createComponent(TalentPoolComponent);
      TestBed.tick();
      const entries: TalentPoolEntry[] = [
        {
          applicationId: 7,
          firstName: 'Lina',
          lastName: 'Pool',
          email: 'lina@example.org',
          jobId: 3,
          jobTitle: 'Data Engineer',
          talentPool: info({ status: 'CONFIRMED', expiresAt: '2028-10-19T09:00:00' }),
        },
        {
          applicationId: 8,
          firstName: 'Otto',
          lastName: 'Still',
          email: null,
          jobId: 4,
          jobTitle: 'DevOps Engineer',
          talentPool: info(),
        },
      ];
      http.expectOne('/api/talent-pool').flush(entries);
      await fixture.whenStable();
      const root = fixture.nativeElement as HTMLElement;

      expect(root.textContent).toContain('1 bestätigt');
      expect(root.textContent).toContain('1 angefragt');
      expect(root.querySelectorAll('tbody tr').length).toBe(2);
      expect(root.querySelector('.row-title')?.getAttribute('href')).toBe('/applications/7');
      expect(root.querySelector('.pool-tag.confirmed')?.textContent).toContain('In Evidenz');
    });
  });

  describe('OutboxComponent', () => {
    it('explains missing mail delivery and renders links as anchors', async () => {
      const fixture = TestBed.createComponent(OutboxComponent);
      TestBed.tick();
      http.expectOne('/api/outbox').flush({
        deliveryEnabled: false,
        mails: [
          {
            id: 1,
            recipient: 'lina@example.org',
            subject: 'Ihre Bewerbung als Data Engineer',
            body: 'Ja:\nhttp://localhost:4200/evidenz/abc?antwort=ja',
            createdAt: '2026-10-18T09:00:00',
            status: 'NOT_SENT',
            error: null,
            applicationId: 7,
          },
        ],
      });
      await fixture.whenStable();
      const root = fixture.nativeElement as HTMLElement;

      expect(root.querySelector('.delivery-hint')?.textContent).toContain('Kein Mailserver');
      expect(root.querySelector('.mail-status')?.textContent).toContain('Nur im Postausgang');
      expect(root.querySelector('.mail-body a')?.getAttribute('href')).toBe(
        'http://localhost:4200/evidenz/abc?antwort=ja',
      );
      expect(root.querySelector('.mail-link')?.getAttribute('href')).toBe('/applications/7');
    });
  });
});
