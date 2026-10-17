import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  computed,
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

export type DocumentSlot = 'resume' | 'skillProfile' | 'coverLetter';

interface OptionalSlot {
  key: Exclude<DocumentSlot, 'resume'>;
  label: string;
  hint: string;
}

const LABELS: Record<DocumentSlot, string> = {
  resume: 'Lebenslauf',
  skillProfile: 'Skill-Profil',
  coverLetter: 'Anschreiben',
};

/**
 * Bewerbungsdialog: Lebenslauf (PDF, Pflicht) sowie optional Skill-Profil und Anschreiben auswählen
 * oder ablegen und absenden.
 */
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

  protected readonly optionalSlots: readonly OptionalSlot[] = [
    {
      key: 'skillProfile',
      label: LABELS.skillProfile,
      hint: 'Ihre Kenntnisse und Technologien im Überblick',
    },
    { key: 'coverLetter', label: LABELS.coverLetter, hint: 'Ihre Motivation für diese Stelle' },
  ];

  protected readonly files = signal<Record<DocumentSlot, File | null>>({
    resume: null,
    skillProfile: null,
    coverLetter: null,
  });
  protected readonly file = computed(() => this.files().resume);
  protected readonly error = signal<string | null>(null);
  protected readonly hotSlot = signal<DocumentSlot | null>(null);
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

  protected onFileSelected(slot: DocumentSlot, input: HTMLInputElement): void {
    this.select(slot, input.files?.[0] ?? null);
    input.value = '';
  }

  protected onDragOver(event: DragEvent, slot: DocumentSlot): void {
    event.preventDefault();
    this.hotSlot.set(slot);
  }

  protected onDrop(event: DragEvent, slot: DocumentSlot): void {
    event.preventDefault();
    this.hotSlot.set(null);
    this.select(slot, event.dataTransfer?.files?.[0] ?? null);
  }

  protected remove(slot: DocumentSlot): void {
    this.files.update((files) => ({ ...files, [slot]: null }));
  }

  protected size(file: File): string {
    return formatMegabytes(file.size);
  }

  protected submit(): void {
    const { resume, skillProfile, coverLetter } = this.files();
    if (!resume || this.submitting()) {
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    this.store.apply(this.job().id, { resume, skillProfile, coverLetter }).subscribe({
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

  private select(slot: DocumentSlot, file: File | null): void {
    if (!file) {
      return;
    }
    const problem = validateFile(file);
    this.error.set(problem && slot !== 'resume' ? `${LABELS[slot]}: ${problem}` : problem);
    if (!problem) {
      this.files.update((files) => ({ ...files, [slot]: file }));
    } else if (slot === 'resume') {
      this.remove(slot);
    }
  }
}
