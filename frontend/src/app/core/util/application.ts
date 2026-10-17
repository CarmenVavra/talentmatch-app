import { ApplicationDetail, ApplicationSummary } from '../models/api.models';

/** Passende Skills zuerst, danach weitere Technologien – ohne Duplikate. */
export function mergeSkills(first: readonly string[], second: readonly string[]): string[] {
  const seen = new Map<string, string>();
  for (const skill of [...first, ...second]) {
    const key = skill.toLowerCase();
    if (!seen.has(key)) {
      seen.set(key, skill);
    }
  }
  return [...seen.values()];
}

/** Wandelt ein Detail (z. B. Antwort von „analyze“) in die Kartendarstellung der Pipeline um. */
export function toSummary(detail: ApplicationDetail): ApplicationSummary {
  return {
    id: detail.id,
    jobPositionId: detail.job.id,
    jobTitle: detail.job.title,
    status: detail.status,
    createdAt: detail.createdAt,
    candidate: {
      id: detail.candidate.id,
      firstName: detail.candidate.firstName,
      lastName: detail.candidate.lastName,
      currentRole: detail.candidate.currentRole,
      currentCompany: detail.candidate.currentCompany,
      city: detail.candidate.city,
    },
    overallScore: detail.analysis?.overallScore ?? null,
    skills: mergeSkills(
      detail.analysis?.matchingSkills ?? [],
      detail.candidate.profile?.technologies ?? [],
    ),
    unread: detail.unread,
  };
}

/** „Rolle · Firma“ bzw. was davon bekannt ist. */
export function positionLabel(role: string | null, company: string | null): string {
  return (
    [role, company].filter((part): part is string => !!part).join(' · ') || 'Position unbekannt'
  );
}

export function fullName(person: { firstName: string; lastName: string }): string {
  return `${person.firstName} ${person.lastName}`;
}
