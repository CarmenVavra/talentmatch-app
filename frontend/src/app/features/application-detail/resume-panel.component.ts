import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { LucideAngularModule } from 'lucide-angular';

import { DocumentKind } from '../../core/models/api.models';
import { ICONS } from '../../shared/icons';
import { SegmentOption, SegmentedControlComponent } from '../../shared/segmented-control.component';
import { ApplicationDetailStore, documentFileName } from './application-detail.store';

type CvTab = 'cv' | 'pdf';

const DOCUMENT_LABELS: Record<DocumentKind, string> = {
  resume: 'Lebenslauf',
  'skill-profile': 'Skill-Profil',
  'cover-letter': 'Anschreiben',
};

/**
 * Linke Spalte: extrahierter Werdegang oder PDF-Vorschau der eingereichten Unterlagen
 * (Lebenslauf sowie – falls vorhanden – Skill-Profil und Anschreiben).
 */
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
  protected readonly document = signal<DocumentKind>('resume');

  protected readonly detail = this.store.detail;
  protected readonly profile = computed(() => this.detail()?.candidate.profile ?? null);

  /** Eingereichte Unterlagen; der Lebenslauf erscheint immer (bei Demo-Daten als generierte Vorschau). */
  protected readonly documentOptions = computed<SegmentOption<DocumentKind>[]>(() => {
    const d = this.detail();
    return (Object.keys(DOCUMENT_LABELS) as DocumentKind[])
      .filter((kind) => kind === 'resume' || (d !== null && documentFileName(d, kind) !== null))
      .map((kind) => ({ value: kind, label: DOCUMENT_LABELS[kind] }));
  });

  protected readonly tabOptions = computed<SegmentOption<CvTab>[]>(() => {
    const count = this.documentOptions().length;
    return [
      { value: 'cv', label: 'Extrahierter Werdegang' },
      { value: 'pdf', label: count > 1 ? `PDF-Vorschau (${count})` : 'PDF-Vorschau' },
    ];
  });

  protected readonly fileName = computed(() => {
    const d = this.detail();
    return d ? documentFileName(d, this.document()) : null;
  });

  protected readonly pdfUrl = computed<SafeResourceUrl | null>(() => {
    const url = this.store.documentUrls()[this.document()];
    // Blob-URL wurde lokal aus der API-Antwort erzeugt und ist daher vertrauenswürdig.
    return url ? this.sanitizer.bypassSecurityTrustResourceUrl(url) : null;
  });

  protected readonly pdfError = computed(
    () => this.store.documentErrors()[this.document()] ?? null,
  );

  protected selectTab(tab: CvTab): void {
    this.tab.set(tab);
    if (tab === 'pdf') {
      this.store.loadDocument(this.document());
    }
  }

  protected selectDocument(kind: DocumentKind): void {
    this.document.set(kind);
    this.store.loadDocument(kind);
  }

  protected period(value: string | null): string {
    return value ?? '';
  }
}
