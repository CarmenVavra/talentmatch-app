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

import { Job } from '../../core/models/api.models';
import { formatMegabytes } from '../../core/util/format';
import { ScoreTone, scoreTone } from '../../core/util/score';
import { ICONS } from '../../shared/icons';
import { UploadItem, UploadStore } from './upload.store';

const STAGE_LABELS: Record<UploadItem['stage'], string> = {
  uploading: 'Wird hochgeladen …',
  uploaded: 'Hochgeladen',
  extracted: 'Text extrahiert',
  analyzing: 'KI-Analyse läuft …',
  done: 'Analysiert · in „KI-analysiert“ einsortiert',
  error: '',
};

/** Modal über der Pipeline: Drag-and-Drop-Upload von PDF-Lebensläufen mit Fortschritt je Datei. */
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
    return formatMegabytes(item.file.size);
  }

  protected scoreToneOf(item: UploadItem): ScoreTone {
    return scoreTone(item.score);
  }
}
