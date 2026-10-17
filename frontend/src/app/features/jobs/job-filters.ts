import { Job } from '../../core/models/api.models';
import { STATUS_COLUMNS } from '../../core/util/score';

export const DEPARTMENTS = ['Engineering', 'Data', 'Product', 'People'] as const;

export interface StatusSegment {
  key: string;
  count: number;
  color: string;
  label: string;
  short: string;
}

/** Suche über Titel, Abteilung und Skills (case-insensitive); Abteilungsfilter exakt. */
export function filterJobs(jobs: readonly Job[], query: string, department: string): Job[] {
  const q = query.trim().toLowerCase();
  return jobs.filter((job) => {
    const matchesDept = department === 'Alle' || job.department === department;
    const haystack = `${job.title} ${job.department} ${job.requiredSkills.join(' ')}`.toLowerCase();
    return matchesDept && (!q || haystack.includes(q));
  });
}

/** Segmente des gestapelten Statusbalkens (nur Status mit Bewerbungen). */
export function statusSegments(job: Job): StatusSegment[] {
  return STATUS_COLUMNS.map((col) => ({
    key: col.key,
    count: job.statusCounts[col.key] ?? 0,
    color: col.color,
    label: col.label,
    short: col.short,
  })).filter((segment) => segment.count > 0);
}
