import { ApplicationStatus } from '../models/api.models';

/** Ampel-Ton eines Match-Scores (Fläche / Text / Balken / Urteil). */
export interface ScoreTone {
  bg: string;
  fg: string;
  bar: string;
  verdict: string;
}

export function scoreTone(score: number | null | undefined): ScoreTone {
  if (score === null || score === undefined) {
    return { bg: '#E9ECEE', fg: '#5F6366', bar: '#9AA0A4', verdict: 'Ausstehend' };
  }
  if (score >= 75) {
    return { bg: '#DCEFE3', fg: '#25603F', bar: '#3F8F5F', verdict: 'Starke Passung' };
  }
  if (score >= 50) {
    return { bg: '#FBEFD0', fg: '#75560E', bar: '#E0A82E', verdict: 'Teilweise Passung' };
  }
  return { bg: '#F8DCD9', fg: '#8E2C26', bar: '#C9473F', verdict: 'Geringe Passung' };
}

export function scoreLabel(score: number | null | undefined): string {
  return score === null || score === undefined ? '—' : String(score);
}

/** Spalten der Kanban-Pipeline in Reihenfolge des Status-Enums. */
export interface StatusMeta {
  key: ApplicationStatus;
  label: string;
  short: string;
  color: string;
  tagBg: string;
  tagFg: string;
}

export const STATUS_COLUMNS: readonly StatusMeta[] = [
  {
    key: 'NEW',
    label: 'Eingegangen',
    short: 'neu',
    color: '#5F6366',
    tagBg: '#E9ECEE',
    tagFg: '#2F4A6E',
  },
  {
    key: 'MATCHED',
    label: 'KI-analysiert',
    short: 'analysiert',
    color: '#86B3D1',
    tagBg: '#E2EDF6',
    tagFg: '#2F4A6E',
  },
  {
    key: 'INTERVIEW',
    label: 'Interview',
    short: 'Interview',
    color: '#99CED3',
    tagBg: '#DDF0F2',
    tagFg: '#2F4A6E',
  },
  {
    key: 'HIRED',
    label: 'Zusage',
    short: 'Zusage',
    color: '#3F8F5F',
    tagBg: '#DCEFE3',
    tagFg: '#25603F',
  },
  {
    key: 'REJECTED',
    label: 'Absage',
    short: 'Absage',
    color: '#EDB5BF',
    tagBg: '#FBE6EA',
    tagFg: '#8A3346',
  },
];

export function statusMeta(status: ApplicationStatus): StatusMeta {
  return STATUS_COLUMNS.find((c) => c.key === status) ?? STATUS_COLUMNS[0];
}

/** Sortierung je Spalte: nicht analysierte zuerst, danach Score absteigend. */
export function compareByScore(a: number | null, b: number | null): number {
  return (b ?? 101) - (a ?? 101);
}
