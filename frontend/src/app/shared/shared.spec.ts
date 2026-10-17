import { TestBed } from '@angular/core/testing';

import { LogoComponent } from './logo.component';
import {
  DayMonthPipe,
  DeDatePipe,
  RelativeTimePipe,
  ScoreLabelPipe,
  ScoreTonePipe,
  StatusMetaPipe,
} from './pipes';
import { SegmentedControlComponent } from './segmented-control.component';

describe('SegmentedControlComponent', () => {
  it('renders options and updates the bound value on selection', async () => {
    const fixture = TestBed.createComponent(SegmentedControlComponent<string>);
    fixture.componentRef.setInput('options', [
      { value: 'tiles', label: 'Kacheln' },
      { value: 'table', label: 'Tabelle' },
    ]);
    fixture.componentRef.setInput('value', 'tiles');
    await fixture.whenStable();

    const inputs = fixture.nativeElement.querySelectorAll('input') as NodeListOf<HTMLInputElement>;
    expect(inputs.length).toBe(2);
    expect(inputs[0].checked).toBeTrue();

    inputs[1].click();
    await fixture.whenStable();
    expect(fixture.componentInstance.value()).toBe('table');
  });
});

describe('LogoComponent', () => {
  it('renders four colour squares', async () => {
    const fixture = TestBed.createComponent(LogoComponent);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelectorAll('.logo > span').length).toBe(4);
  });
});

describe('pipes', () => {
  it('delegate to the formatting helpers', () => {
    expect(new ScoreTonePipe().transform(80).verdict).toBe('Starke Passung');
    expect(new ScoreLabelPipe().transform(null)).toBe('—');
    expect(new StatusMetaPipe().transform('NEW').label).toBe('Eingegangen');
    expect(new DeDatePipe().transform('2026-09-01')).toBe('01.09.2026');
    expect(new DeDatePipe().transform(null)).toBe('');
    expect(new DayMonthPipe().transform('2026-10-17T10:00:00')).toBe('17.10.');
    expect(new RelativeTimePipe().transform(new Date().toISOString())).toBe('gerade eben');
  });
});
