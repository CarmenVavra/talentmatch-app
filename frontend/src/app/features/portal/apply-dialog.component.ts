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
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

import { MyApplication, OpenJob } from '../../core/models/api.models';
import { validateFile } from '../../core/util/file';
import { formatMegabytes } from '../../core/util/format';
import { errorMessage } from '../../core/util/http-error';
import { ICONS } from '../../shared/icons';
import { PortalStore } from './portal.store';

/** Bewerbungsdialog: Lebenslauf (PDF) auswählen oder ablegen und absenden. */
@Component({
  selector: 'app-apply-dialog',
  imports: [LucideAngularModule, RouterLink],
  templateUrl: './apply-dialog.component.html',
  styleUrl: './apply-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown.escape)': 'close()' },
})
export class ApplyDialogComponent implements AfterViewInit {
  private readonly store = inject(PortalStore);
  protected readonly icons = ICONS;

  readonly job = input.required<OpenJob>();
  readonly closed = output<void>();

  protected readonly file = signal<File | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly zoneHot = signal(false);
  protected readonly submitting = signal(false);
  protected readonly submitted = signal<MyApplication | null>(null);

  private readonly fileInput = viewChild.required<ElementRef<HTMLInputElement>>('fileInput');
  private readonly dialog = viewChild.required<ElementRef<HTMLElement>>('dialog');

  ngAfterViewInit(): void {
    this.dialog().nativeElement.focus();
  }

  protected close(): void {
    if (!this.submitting()) {
      this.closed.emit();
    }
  }

  protected onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.close();
    }
  }

  protected pickFile(): void {
    this.fileInput().nativeElement.click();
  }

  protected onFileSelected(input: HTMLInputElement): void {
    this.select(input.files?.[0] ?? null);
    input.value = '';
  }

  protected onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.zoneHot.set(true);
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    this.zoneHot.set(false);
    this.select(event.dataTransfer?.files?.[0] ?? null);
  }

  protected size(file: File): string {
    return formatMegabytes(file.size);
  }

  protected submit(): void {
    const file = this.file();
    if (!file || this.submitting()) {
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    this.store.apply(this.job().id, file).subscribe({
      next: (application) => {
        this.submitting.set(false);
        this.submitted.set(application);
      },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.error.set(errorMessage(err, 'Die Bewerbung konnte nicht gesendet werden.'));
      },
    });
  }

  private select(file: File | null): void {
    if (!file) {
      return;
    }
    const problem = validateFile(file);
    this.error.set(problem);
    this.file.set(problem ? null : file);
  }
}
