import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { makeSummary } from '../../testing/test-data';
import { CandidateListComponent, filterApplications } from './candidate-list.component';

const APPS = [
  makeSummary({ id: 1, status: 'MATCHED', overallScore: 84 }),
  makeSummary({
    id: 2,
    status: 'NEW',
    overallScore: null,
    jobTitle: 'Data Engineer',
    candidate: { ...makeSummary().candidate, firstName: 'Anna', lastName: 'Müller' },
  }),
  makeSummary({ id: 3, status: 'HIRED', overallScore: 91 }),
];

describe('filterApplications', () => {
  it('filters by status and searches name, job and skills', () => {
    expect(filterApplications(APPS, '', 'NEW').map((a) => a.id)).toEqual([2]);
    expect(filterApplications(APPS, 'data engineer', 'ALL').map((a) => a.id)).toEqual([2]);
    expect(filterApplications(APPS, 'müller', 'ALL').map((a) => a.id)).toEqual([2]);
    expect(filterApplications(APPS, 'postgresql', 'HIRED').map((a) => a.id)).toEqual([3]);
  });
});

describe('CandidateListComponent', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function render() {
    const fixture = TestBed.createComponent(CandidateListComponent);
    // Keine whenStable() vor dem Flush: die laufende Resource zählt als ausstehende Aufgabe.
    TestBed.tick();
    http.expectOne('/api/applications').flush(APPS);
    await fixture.whenStable();
    return fixture;
  }

  it('lists all applications and opens the detail on click', async () => {
    const fixture = await render();
    const rows = fixture.nativeElement.querySelectorAll('tbody tr');
    expect(rows.length).toBe(3);
    expect(fixture.nativeElement.textContent).toContain('3 von 3 Bewerbungen');

    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    (rows[0] as HTMLElement).click();
    expect(navigate).toHaveBeenCalledWith(['/applications', 1]);
  });

  it('sorts by score with pending applications last', async () => {
    const fixture = await render();
    const sortInputs = fixture.nativeElement.querySelectorAll('.sort input');
    (sortInputs[1] as HTMLInputElement).click();
    await fixture.whenStable();
    const scores = Array.from(
      fixture.nativeElement.querySelectorAll('.score-badge') as NodeListOf<HTMLElement>,
    ).map((e) => e.textContent?.trim());
    expect(scores).toEqual(['91', '84', '—']);
  });
});
