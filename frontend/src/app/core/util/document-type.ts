import { DocumentType } from '../models/api.models';

const LABELS: Record<DocumentType, string> = {
  RESUME: 'Lebenslauf',
  SKILL_PROFILE: 'Skill-Profil',
  COVER_LETTER: 'Anschreiben',
};

/** Alle Dokumentarten in Anzeigereihenfolge. */
export const DOCUMENT_TYPES: readonly DocumentType[] = ['RESUME', 'SKILL_PROFILE', 'COVER_LETTER'];

export function documentTypeLabel(type: DocumentType): string {
  return LABELS[type];
}
