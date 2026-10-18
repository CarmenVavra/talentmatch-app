import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';

import { DocumentType, GroupMatch, Job } from '../../core/models/api.models';
import { formatMegabytes } from '../../core/util/format';
import { ScoreTone, scoreTone } from '../../core/util/score';
import { ICONS } from '../../shared/icons';
import { DOCUMENT_TYPES, documentTypeLabel } from '../../core/util/document-type';
import { DraftGroup, UploadItem, UploadStore, groupLabel, groupProblem } from './upload.store';

const MATCH_LABELS: Record<GroupMatch, string> = {
  EMAIL: 'gleiche E-Mail-Adresse',
  PHONE: 'gleiche Telefonnummer',
  NAME: 'gleicher Name',
  NONE: '',
};

const STAGE_LABELS: Record<UploadItem['stage'], string> = {
  uploading: 'Wird hochgeladen …',
  uploaded: 'Hochgeladen',
  extracted: 'Text extrahiert',
  analyzing: 'KI-Analyse läuft …',
  done: 'Analysiert · in „KI-analysiert“ einsortiert',
  error: '',
};

/**
 * Modal über der Pipeline: PDFs ablegen → Vorschau mit den erkannten Personen und Dokumentarten (korrigierbar)
 * → je Person eine Bewerbung hochladen, mit Fortschritt.
 */
@Component({
  selector: 'app-upload-dialog',
  imports: [LucideAngularModule],
  templateUrl: './upload-dialog.component.html',
  styleUrl: './upload-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown.escape)': 'closed.emit()' },
})
export class UploadDialogComponent implements AfterViewInit {
  protected readonly uploads = inject(UploadStore);
  protected readonly icons = ICONS;

  readonly job = input.required<Job>();
  readonly closed = output<void>();

  protected readonly zoneHot = signal(false);
  protected readonly documentTypes = DOCUMENT_TYPES;
  protected readonly typeLabel = documentTypeLabel;
  protected readonly groupLabel = groupLabel;
  protected readonly groupProblem = groupProblem;
  private readonly fileInput = viewChild.required<ElementRef<HTMLInputElement>>('fileInput');
  private readonly dialog = viewChild.required<ElementRef<HTMLElement>>('dialog');

  ngAfterViewInit(): void {
    this.dialog().nativeElement.focus();
  }

  /** Nur ein Klick direkt auf den Hintergrund schließt den Dialog. */
  protected onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.closed.emit();
    }
  }

  protected pickFiles(): void {
    this.fileInput().nativeElement.click();
  }

  protected onFilesSelected(input: HTMLInputElement): void {
    if (input.files?.length) {
      this.uploads.addFiles(this.job().id, Array.from(input.files));
    }
    input.value = '';
  }

  protected onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.zoneHot.set(true);
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    this.zoneHot.set(false);
    const files = event.dataTransfer?.files;
    if (files?.length) {
      this.uploads.addFiles(this.job().id, Array.from(files));
    }
  }

  protected matchLabel(group: DraftGroup): string {
    return MATCH_LABELS[group.match];
  }

  protected onTypeChange(fileKey: number, select: HTMLSelectElement): void {
    this.uploads.setType(fileKey, select.value as DocumentType);
  }

  protected onGroupChange(fileKey: number, select: HTMLSelectElement): void {
    this.uploads.moveFile(fileKey, select.value === 'new' ? null : Number(select.value));
  }

  protected stageLabel(item: UploadItem): string {
    return item.stage === 'error' ? (item.error ?? 'Fehler') : STAGE_LABELS[item.stage];
  }

  protected barColor(item: UploadItem): string {
    if (item.stage === 'error') {
      return '#C9473F';
    }
    return item.stage === 'done' ? '#3F8F5F' : '#4D6D9A';
  }

  protected size(item: UploadItem): string {
    return formatMegabytes(item.sizeBytes);
  }

  protected scoreToneOf(item: UploadItem): ScoreTone {
    return scoreTone(item.score);
  }
}
