import { TalentPoolInfo, TalentPoolStatus } from '../models/api.models';
import { formatDate } from './format';

const LABELS: Record<TalentPoolStatus, string> = {
  REQUESTED: 'Evidenz angefragt',
  CONFIRMED: 'In Evidenz',
};

export function talentPoolLabel(status: TalentPoolStatus): string {
  return LABELS[status];
}

/** Kurzbeschreibung des Evidenz-Stands für HR, z. B. „Antwort bis 01.11.2026“. */
export function talentPoolDetail(info: TalentPoolInfo): string {
  if (info.status === 'CONFIRMED') {
    const since = info.confirmedAt ? `zugestimmt am ${formatDate(info.confirmedAt)}` : 'zugestimmt';
    return info.expiresAt ? `${since} · gilt bis ${formatDate(info.expiresAt)}` : since;
  }
  const reminder = info.reminderSentAt ? ` · Erinnerung am ${formatDate(info.reminderSentAt)}` : '';
  return `angefragt am ${formatDate(info.requestedAt)} · Antwort bis ${formatDate(info.responseDeadline)}${reminder}`;
}

/** Textabschnitt; {@code href} ist für erkannte http(s)-Links gesetzt. */
export interface TextPart {
  text: string;
  href: string | null;
}

const URL_PATTERN = /https?:\/\/[^\s<>"']+/g;

/**
 * Zerlegt E-Mail-Text in Text und Links – gerendert als Text bzw. {@code <a>}, nie als HTML.
 */
export function linkify(text: string): TextPart[] {
  const parts: TextPart[] = [];
  let last = 0;
  for (const match of text.matchAll(URL_PATTERN)) {
    const start = match.index ?? 0;
    if (start > last) {
      parts.push({ text: text.slice(last, start), href: null });
    }
    parts.push({ text: match[0], href: match[0] });
    last = start + match[0].length;
  }
  if (last < text.length) {
    parts.push({ text: text.slice(last), href: null });
  }
  return parts;
}
