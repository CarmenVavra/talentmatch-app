import { TestBed } from '@angular/core/testing';

import { makeDetail, makeDuplicate } from '../../testing/test-data';
import { MergeDialogComponent, defaultTargets } from './merge-dialog.component';

describe('defaultTargets', () => {
  it('uses the suggested type when that slot is still free', () => {
    expect(defaultTargets(makeDetail({ resumeFileName: 'cv.pdf' }), makeDuplicate())).toEqual([
      'SKILL_PROFILE',
    ]);
  });

  it('never replaces an existing document without asking', () => {
    const target = makeDetail({ resumeFileName: 'cv.pdf', skillProfileFileName: 'skills.pdf' });
    expect(defaultTargets(target, makeDuplicate())).toEqual(['SKIP']);
  });

  it('assigns each type only once', () => {
    const source = makeDuplicate({
      documents: [
        { type: 'RESUME', fileName: 'a.pdf', suggestedAs: 'COVER_LETTER' },
        { type: 'COVER_LETTER', fileName: 'b.pdf', suggestedAs: 'COVER_LETTER' },
      ],
    });
    expect(defaultTargets(makeDetail(), source)).toEqual(['COVER_LETTER', 'SKIP']);
  });
});

describe('MergeDialogComponent', () => {
  it('warns about replacements and blocks duplicate target types', async () => {
    const fixture = TestBed.createComponent(MergeDialogComponent);
    fixture.componentRef.setInput(
      'target',
      makeDetail({ resumeFileName: 'cv.pdf', skillProfileFileName: 'alt.pdf' }),
    );
    fixture.componentRef.setInput(
      'source',
      makeDuplicate({
        documents: [
          { type: 'RESUME', fileName: 'neu-cv.pdf', suggestedAs: 'RESUME' },
          { type: 'SKILL_PROFILE', fileName: 'neu-skills.pdf', suggestedAs: 'SKILL_PROFILE' },
        ],
      }),
    );
    let request: unknown = null;
    fixture.componentInstance.confirmed.subscribe((r) => (request = r));
    await fixture.whenStable();
    const root = fixture.nativeElement as HTMLElement;
    expect(root.querySelector('.scope')?.textContent).toContain('„Senior Java Developer“');
    const selects = Array.from(root.querySelectorAll('.move-select')) as HTMLSelectElement[];
    expect(selects.map((s) => s.value)).toEqual(['SKIP', 'SKIP']);

    selects[1].value = 'SKILL_PROFILE';
    selects[1].dispatchEvent(new Event('change'));
    await fixture.whenStable();
    expect(root.querySelector('.replaces')?.textContent).toContain('alt.pdf');

    selects[0].value = 'SKILL_PROFILE';
    selects[0].dispatchEvent(new Event('change'));
    await fixture.whenStable();
    expect(root.querySelector('.merge-problem')?.textContent).toContain('nur einmal');
    expect((root.querySelector('.confirm') as HTMLButtonElement).disabled).toBeTrue();

    selects[0].value = 'SKIP';
    selects[0].dispatchEvent(new Event('change'));
    await fixture.whenStable();
    (root.querySelector('.confirm') as HTMLButtonElement).click();
    expect(request).toEqual({
      sourceId: 20,
      documents: [{ from: 'SKILL_PROFILE', to: 'SKILL_PROFILE' }],
    });
  });
});
