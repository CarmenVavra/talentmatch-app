import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { LucideAngularModule } from 'lucide-angular';

import { ICONS } from '../../shared/icons';
import { SegmentOption, SegmentedControlComponent } from '../../shared/segmented-control.component';
import { ApplicationDetailStore } from './application-detail.store';

type CvTab = 'cv' | 'pdf';

/** Linke Spalte: extrahierter Werdegang oder PDF-Vorschau der Originaldatei. */
@Component({
  selector: 'app-resume-panel',
  imports: [LucideAngularModule, SegmentedControlComponent],
  templateUrl: './resume-panel.component.html',
  styleUrl: './resume-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ResumePanelComponent {
  protected readonly store = inject(ApplicationDetailStore);
  private readonly sanitizer = inject(DomSanitizer);
  protected readonly icons = ICONS;

  protected readonly tab = signal<CvTab>('cv');
  protected readonly tabOptions: SegmentOption<CvTab>[] = [
    { value: 'cv', label: 'Extrahierter Werdegang' },
    { value: 'pdf', label: 'PDF-Vorschau' },
  ];

  protected readonly detail = this.store.detail;
  protected readonly profile = computed(() => this.detail()?.candidate.profile ?? null);
  protected readonly pdfUrl = computed<SafeResourceUrl | null>(() => {
    const url = this.store.resumeUrl();
    // Blob-URL wurde lokal aus der API-Antwort erzeugt und ist daher vertrauenswürdig.
    return url ? this.sanitizer.bypassSecurityTrustResourceUrl(url) : null;
  });

  protected selectTab(tab: CvTab): void {
    this.tab.set(tab);
    if (tab === 'pdf') {
      this.store.loadResume();
    }
  }

  protected period(value: string | null): string {
    return value ?? '';
  }
}
