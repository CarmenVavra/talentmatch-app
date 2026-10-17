import { HttpErrorResponse } from '@angular/common/http';

import { makeDetail } from '../../testing/test-data';
import { mergeSkills, positionLabel, toSummary } from './application';
import { formatDate, formatDayMonth, formatMegabytes, relativeTime } from './format';
import { errorMessage, fieldErrors } from './http-error';
import { compareByScore, scoreLabel, scoreTone, statusMeta } from './score';

describe('score utils', () => {
  it('maps scores to traffic-light tones', () => {
    expect(scoreTone(91).verdict).toBe('Starke Passung');
    expect(scoreTone(75).bar).toBe('#3F8F5F');
    expect(scoreTone(74).verdict).toBe('Teilweise Passung');
    expect(scoreTone(50).fg).toBe('#75560E');
    expect(scoreTone(49).verdict).toBe('Geringe Passung');
    expect(scoreTone(null).verdict).toBe('Ausstehend');
  });

  it('labels missing scores with a dash', () => {
    expect(scoreLabel(null)).toBe('—');
    expect(scoreLabel(0)).toBe('0');
  });

  it('sorts unanalysed first, then by score descending', () => {
    const scores = [61, null, 91, 72].sort(compareByScore);
    expect(scores).toEqual([null, 91, 72, 61]);
  });

  it('provides status metadata', () => {
    expect(statusMeta('HIRED').label).toBe('Zusage');
    expect(statusMeta('REJECTED').tagFg).toBe('#8A3346');
  });
});

describe('format utils', () => {
  it('formats dates in German notation', () => {
    expect(formatDate('2026-10-02')).toBe('02.10.2026');
    expect(formatDayMonth('2026-10-04T09:00:00')).toBe('04.10.');
  });

  it('formats relative times', () => {
    const now = new Date('2026-10-17T12:00:00Z');
    expect(relativeTime('2026-10-17T11:59:50Z', now)).toBe('gerade eben');
    expect(relativeTime('2026-10-17T11:30:00Z', now)).toBe('vor 30 Min.');
    expect(relativeTime('2026-10-17T10:00:00Z', now)).toBe('vor 2 Std.');
    expect(relativeTime('2026-10-16T12:00:00Z', now)).toBe('vor 1 Tag');
    expect(relativeTime('2026-10-12T12:00:00Z', now)).toBe('vor 5 Tagen');
  });

  it('formats file sizes in MB', () => {
    expect(formatMegabytes(842000)).toBe('0.8 MB');
  });
});

describe('application utils', () => {
  it('merges skills case-insensitively keeping the first spelling', () => {
    expect(mergeSkills(['Java', 'AWS'], ['java', 'Kafka'])).toEqual(['Java', 'AWS', 'Kafka']);
  });

  it('builds a position label from the known parts', () => {
    expect(positionLabel('Java Developer', 'Siemens')).toBe('Java Developer · Siemens');
    expect(positionLabel(null, 'Siemens')).toBe('Siemens');
    expect(positionLabel(null, null)).toBe('Position unbekannt');
  });

  it('converts a detail into a pipeline summary', () => {
    const summary = toSummary(makeDetail());
    expect(summary.overallScore).toBe(91);
    expect(summary.skills).toEqual(['Java', 'Spring Boot', 'Java 17', 'Spring Boot 3', 'Kafka']);
    expect(summary.jobPositionId).toBe(1);
  });

  it('converts an unanalysed detail with null score', () => {
    expect(toSummary(makeDetail({ analysis: null })).overallScore).toBeNull();
  });
});

describe('http error utils', () => {
  it('reads the detail of a problem response', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: { title: 'Ungültige Datei', detail: 'Nur PDF', errors: { file: 'fehlt' } },
    });
    expect(errorMessage(error)).toBe('Nur PDF');
    expect(fieldErrors(error)).toEqual({ file: 'fehlt' });
  });

  it('explains an unreachable server', () => {
    expect(errorMessage(new HttpErrorResponse({ status: 0 }))).toContain('nicht erreichbar');
  });

  it('falls back for unknown errors', () => {
    expect(errorMessage(new Error('x'), 'Fallback')).toBe('Fallback');
    expect(fieldErrors(new Error('x'))).toEqual({});
  });
});
