import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';

import {
  ApplicationDetail,
  DocumentType,
  Duplicate,
  MergeRequest,
} from '../../core/models/api.models';
import { DOCUMENT_TYPES, documentTypeLabel } from '../../core/util/document-type';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { DayMonthPipe } from '../../shared/pipes';

export type MergeTarget = DocumentType | 'SKIP';

const REASONS: Record<Duplicate['reason'], string> = {
  EMAIL: 'gleiche E-Mail-Adresse',
  PHONE: 'gleiche Telefonnummer',
  NAME: 'gleicher Name',
  NONE: '',
};

export function duplicateReason(duplicate: Duplicate): string {
  return REASONS[duplicate.reason];
}

/** Dateiname der Unterlage, die in der Ziel-Bewerbung bereits an dieser Stelle liegt. */
function existingFile(target: ApplicationDetail, type: DocumentType): string | null {
  switch (type) {
    case 'RESUME':
      return target.resumeFileName;
    case 'SKILL_PROFILE':
      return target.skillProfileFileName;
    case 'COVER_LETTER':
      return target.coverLetterFileName;
  }
}

/**
 * Standardauswahl: als vorgeschlagene Dokumentart übernehmen, sofern diese Stelle in der Ziel-Bewerbung
 * noch frei ist – sonst nicht übernehmen (nichts wird ungefragt ersetzt).
 */
export function defaultTargets(target: ApplicationDetail, source: Duplicate): MergeTarget[] {
  const taken = new Set<DocumentType>();
  return source.documents.map((doc) => {
    const free = !existingFile(target, doc.suggestedAs) && !taken.has(doc.suggestedAs);
    if (free) {
      taken.add(doc.suggestedAs);
      return doc.suggestedAs;
    }
    return 'SKIP';
  });
}

/** Dialog: Unterlagen eines Duplikats in diese Bewerbung übernehmen und das Duplikat löschen. */
@Component({
  selector: 'app-merge-dialog',
  imports: [ConfirmDialogComponent, DayMonthPipe],
  templateUrl: './merge-dialog.component.html',
  styleUrl: './merge-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MergeDialogComponent {
  readonly target = input.required<ApplicationDetail>();
  readonly source = input.required<Duplicate>();
  readonly busy = input(false);

  readonly confirmed = output<MergeRequest>();
  readonly cancelled = output<void>();

  protected readonly types = DOCUMENT_TYPES;
  protected readonly typeLabel = documentTypeLabel;
  protected readonly reason = duplicateReason;

  /** Gewählte Zielart je Unterlage der Quelle (null = noch Standard). */
  private readonly chosen = signal<MergeTarget[] | null>(null);
  protected readonly targets = computed(
    () => this.chosen() ?? defaultTargets(this.target(), this.source()),
  );

  protected readonly problem = computed(() => {
    const used = this.targets().filter((t): t is DocumentType => t !== 'SKIP');
    return new Set(used).size === used.length
      ? null
      : 'Jede Dokumentart kann nur einmal übernommen werden.';
  });

  protected select(index: number, select: HTMLSelectElement): void {
    const next = [...this.targets()];
    next[index] = select.value as MergeTarget;
    this.chosen.set(next);
  }

  /** Hinweis, welche vorhandene Datei durch die Übernahme ersetzt würde. */
  protected replaces(target: MergeTarget): string | null {
    return target === 'SKIP' ? null : existingFile(this.target(), target);
  }

  protected confirm(): void {
    if (this.problem()) {
      return;
    }
    this.confirmed.emit({
      sourceId: this.source().id,
      documents: this.source()
        .documents.map((doc, i) => ({ from: doc.type, to: this.targets()[i] }))
        .filter((move): move is { from: DocumentType; to: DocumentType } => move.to !== 'SKIP'),
    });
  }
}
