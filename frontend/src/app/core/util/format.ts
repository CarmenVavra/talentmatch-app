/** Datums- und Größenformatierung im deutschen Format. */

function toDate(value: string | Date): Date {
  if (value instanceof Date) {
    return value;
  }
  // Reine Datumsangaben (YYYY-MM-DD) als lokales Datum interpretieren, nicht als UTC.
  const dateOnly = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  if (dateOnly) {
    return new Date(Number(dateOnly[1]), Number(dateOnly[2]) - 1, Number(dateOnly[3]));
  }
  return new Date(value);
}

function pad(value: number): string {
  return String(value).padStart(2, '0');
}

/** TT.MM.JJJJ */
export function formatDate(value: string | Date): string {
  const d = toDate(value);
  return `${pad(d.getDate())}.${pad(d.getMonth() + 1)}.${d.getFullYear()}`;
}

/** TT.MM. */
export function formatDayMonth(value: string | Date): string {
  const d = toDate(value);
  return `${pad(d.getDate())}.${pad(d.getMonth() + 1)}.`;
}

/** „vor 5 Min.“, „vor 2 Std.“, „vor 3 Tagen“ */
export function relativeTime(value: string | Date, now: Date = new Date()): string {
  const diffMinutes = Math.max(0, Math.round((now.getTime() - toDate(value).getTime()) / 60000));
  if (diffMinutes < 1) {
    return 'gerade eben';
  }
  if (diffMinutes < 60) {
    return `vor ${diffMinutes} Min.`;
  }
  const hours = Math.round(diffMinutes / 60);
  if (hours < 24) {
    return `vor ${hours} Std.`;
  }
  const days = Math.round(hours / 24);
  return days === 1 ? 'vor 1 Tag' : `vor ${days} Tagen`;
}

/** Dateigröße als „x.x MB“. */
export function formatMegabytes(bytes: number): string {
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

const COUNT = new Intl.NumberFormat('de-DE');

/** Ganzzahl mit deutschem Tausenderpunkt, z. B. 20.000. */
export function formatCount(value: number): string {
  return COUNT.format(value);
}
