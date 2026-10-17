import { TestBed } from '@angular/core/testing';

import { parseBold, wrapBold } from '../core/util/rich-text';
import { RichTextComponent } from './rich-text.component';

describe('parseBold', () => {
  it('splits text into plain and bold segments', () => {
    expect(parseBold('**Ihre Aufgaben**\nEntwicklung mit **Angular** und C#')).toEqual([
      { text: 'Ihre Aufgaben', bold: true },
      { text: '\nEntwicklung mit ', bold: false },
      { text: 'Angular', bold: true },
      { text: ' und C#', bold: false },
    ]);
  });

  it('keeps unmatched or spaced stars as plain text', () => {
    expect(parseBold('2 ** 3 und **offen')).toEqual([{ text: '2 ** 3 und **offen', bold: false }]);
    expect(parseBold('** kein fett **')).toEqual([{ text: '** kein fett **', bold: false }]);
  });

  it('does not span line breaks', () => {
    expect(parseBold('**Zeile 1\nZeile 2**')).toEqual([
      { text: '**Zeile 1\nZeile 2**', bold: false },
    ]);
  });

  it('returns nothing for empty text', () => {
    expect(parseBold('')).toEqual([]);
  });
});

describe('wrapBold', () => {
  it('wraps the selection and keeps it selected', () => {
    expect(wrapBold('Ihre Aufgaben folgen', 0, 13)).toEqual({
      text: '**Ihre Aufgaben** folgen',
      start: 2,
      end: 15,
    });
  });

  it('leaves surrounding spaces of the selection outside the stars', () => {
    expect(wrapBold('mit Angular und', 3, 12).text).toBe('mit **Angular** und');
  });

  it('inserts a placeholder without selection', () => {
    expect(wrapBold('Text', 4, 4)).toEqual({ text: 'Text**fetter Text**', start: 6, end: 17 });
  });
});

describe('RichTextComponent', () => {
  it('renders bold segments as strong and never interprets HTML', async () => {
    const fixture = TestBed.createComponent(RichTextComponent);
    fixture.componentRef.setInput('text', '<img src=x onerror=alert(1)> **fett** normal');
    await fixture.whenStable();
    const root = fixture.nativeElement as HTMLElement;

    expect(root.querySelector('img')).toBeNull();
    expect(root.querySelector('strong')?.textContent).toBe('fett');
    expect(root.textContent).toBe('<img src=x onerror=alert(1)> fett normal');
  });
});
