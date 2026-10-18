import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { PrivacyComponent } from './privacy.component';

describe('PrivacyComponent', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
  });

  it('explains the AI analysis, retention periods and self-service deletion', async () => {
    const fixture = TestBed.createComponent(PrivacyComponent);
    await fixture.whenStable();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('OpenAI');
    expect(text).toContain('6 Monaten');
    expect(text).toContain('14 Tagen');
    expect(text).toContain('24 Monate');
    expect(text).toContain('Bewerbung zurückziehen');
    expect(text).toContain('Konto löschen');
  });

  it('links back to the login without a session', async () => {
    const fixture = TestBed.createComponent(PrivacyComponent);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.brand')?.getAttribute('href')).toBe('/login');
  });
});
