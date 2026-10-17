import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  effect,
  inject,
  input,
  numberAttribute,
  signal,
  untracked,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

import { CreateJobRequest, Job } from '../../core/models/api.models';
import { JobStore } from '../../core/state/job.store';
import { ToastService } from '../../core/state/toast.service';
import { errorMessage, fieldErrors } from '../../core/util/http-error';
import { ICONS } from '../../shared/icons';
import { SegmentOption, SegmentedControlComponent } from '../../shared/segmented-control.component';
import { DEPARTMENTS } from './job-filters';

const LEVELS = ['Junior', 'Mid', 'Senior', 'Lead'] as const;

function atLeastOneSkill(control: AbstractControl<string[]>): ValidationErrors | null {
  return control.value.length > 0 ? null : { required: true };
}

function optionalId(value: string | undefined): number | undefined {
  return value === undefined ? undefined : numberAttribute(value);
}

/** Stelle anlegen ({@code /jobs/new}) oder bearbeiten ({@code /jobs/:jobId/edit}). */
@Component({
  selector: 'app-job-create',
  imports: [ReactiveFormsModule, RouterLink, LucideAngularModule, SegmentedControlComponent],
  templateUrl: './job-create.component.html',
  styleUrl: './job-create.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class JobCreateComponent {
  private readonly store = inject(JobStore);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly icons = ICONS;

  /** Routen-Parameter :jobId – nur im Bearbeiten-Modus gesetzt. */
  readonly jobId = input<number | undefined, string | undefined>(undefined, {
    transform: optionalId,
  });
  protected readonly editing = computed(() => this.jobId() !== undefined);
  /** Geladene Stelle im Bearbeiten-Modus. */
  protected readonly job = signal<Job | null>(null);
  protected readonly loadError = signal<string | null>(null);

  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly serverErrors = signal<Record<string, string>>({});
  protected readonly department = signal<string>(DEPARTMENTS[0]);
  protected readonly level = signal<string>('Mid');

  /** Feste Abteilungen plus ggf. die abweichende Abteilung einer bestehenden Stelle. */
  protected readonly departmentOptions = computed<SegmentOption<string>[]>(() => {
    const current = this.job()?.department;
    const all: string[] = [...DEPARTMENTS];
    if (current && !all.includes(current)) {
      all.push(current);
    }
    return all.map((d) => ({ value: d, label: d }));
  });
  protected readonly levelOptions: SegmentOption<string>[] = LEVELS.map((l) => ({
    value: l,
    label: l,
  }));

  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly form = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    location: ['', Validators.required],
    description: ['', Validators.maxLength(4000)],
    skillInput: [''],
    requiredSkills: this.fb.control<string[]>([], atLeastOneSkill),
  });

  constructor() {
    effect(() => {
      const id = this.jobId();
      if (id !== undefined) {
        untracked(() => this.loadJob(id));
      }
    });
  }

  protected addSkill(): void {
    const raw = this.form.controls.skillInput.value;
    const additions = raw
      .split(',')
      .map((s) => s.trim())
      .filter((s) => s.length > 0);
    if (additions.length === 0) {
      return;
    }
    const current = this.form.controls.requiredSkills.value;
    const merged = [...current];
    for (const skill of additions) {
      if (!merged.some((s) => s.toLowerCase() === skill.toLowerCase()) && merged.length < 20) {
        merged.push(skill);
      }
    }
    this.form.controls.requiredSkills.setValue(merged);
    this.form.controls.requiredSkills.markAsTouched();
    this.form.controls.skillInput.setValue('');
  }

  protected onSkillKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' || event.key === ',') {
      event.preventDefault();
      this.addSkill();
    } else if (event.key === 'Backspace' && this.form.controls.skillInput.value === '') {
      const skills = this.form.controls.requiredSkills.value;
      if (skills.length) {
        this.removeSkill(skills[skills.length - 1]);
      }
    }
  }

  protected removeSkill(skill: string): void {
    this.form.controls.requiredSkills.setValue(
      this.form.controls.requiredSkills.value.filter((s) => s !== skill),
    );
    this.form.controls.requiredSkills.markAsTouched();
  }

  protected submit(): void {
    this.addSkill();
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    this.serverErrors.set({});
    const value = this.form.getRawValue();
    const request: CreateJobRequest = {
      title: value.title.trim(),
      department: this.department(),
      experienceLevel: this.level(),
      location: value.location.trim(),
      description: value.description.trim(),
      requiredSkills: value.requiredSkills,
    };
    const id = this.jobId();
    const save$ = id === undefined ? this.store.create(request) : this.store.update(id, request);
    save$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (job) => {
        if (id !== undefined) {
          this.toast.success('Die Änderungen wurden gespeichert.');
        }
        void this.router.navigate(['/jobs', job.id, 'pipeline']);
      },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.serverErrors.set(fieldErrors(err));
        this.error.set(
          errorMessage(
            err,
            id === undefined
              ? 'Die Stelle konnte nicht angelegt werden.'
              : 'Die Änderungen konnten nicht gespeichert werden.',
          ),
        );
      },
    });
  }

  private loadJob(id: number): void {
    this.loadError.set(null);
    this.store
      .fetch(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (job) => {
          this.job.set(job);
          this.department.set(job.department);
          this.level.set(job.experienceLevel);
          this.form.reset({
            title: job.title,
            location: job.location,
            description: job.description ?? '',
            skillInput: '',
            requiredSkills: job.requiredSkills,
          });
        },
        error: (err: unknown) =>
          this.loadError.set(errorMessage(err, 'Die Stelle konnte nicht geladen werden.')),
      });
  }
}
