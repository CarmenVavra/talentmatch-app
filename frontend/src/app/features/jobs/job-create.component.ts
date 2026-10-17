import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

import { JobStore } from '../../core/state/job.store';
import { errorMessage, fieldErrors } from '../../core/util/http-error';
import { ICONS } from '../../shared/icons';
import { SegmentOption, SegmentedControlComponent } from '../../shared/segmented-control.component';
import { DEPARTMENTS } from './job-filters';

const LEVELS = ['Junior', 'Mid', 'Senior', 'Lead'] as const;

function atLeastOneSkill(control: AbstractControl<string[]>): ValidationErrors | null {
  return control.value.length > 0 ? null : { required: true };
}

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
  protected readonly icons = ICONS;

  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly serverErrors = signal<Record<string, string>>({});
  protected readonly department = signal<string>(DEPARTMENTS[0]);
  protected readonly level = signal<string>('Mid');

  protected readonly departmentOptions: SegmentOption<string>[] = DEPARTMENTS.map((d) => ({
    value: d,
    label: d,
  }));
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
    this.store
      .create({
        title: value.title.trim(),
        department: this.department(),
        experienceLevel: this.level(),
        location: value.location.trim(),
        description: value.description.trim(),
        requiredSkills: value.requiredSkills,
      })
      .subscribe({
        next: (job) => void this.router.navigate(['/jobs', job.id, 'pipeline']),
        error: (err: unknown) => {
          this.submitting.set(false);
          this.serverErrors.set(fieldErrors(err));
          this.error.set(errorMessage(err, 'Die Stelle konnte nicht angelegt werden.'));
        },
      });
  }
}
