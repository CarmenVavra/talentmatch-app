/** Abschnitt eines Textes mit einfacher Auszeichnung. */
export interface TextSegment {
  text: string;
  bold: boolean;
}

/**
 * `**fett**` innerhalb einer Zeile – wie in Markdown ohne Leerzeichen direkt innerhalb der Sterne,
 * damit z. B. „2 ** 3“ unverändert bleibt.
 */
const BOLD = /\*\*(?!\s)(.+?)(?<!\s)\*\*/g;

/**
 * Zerlegt einen Text in normale und fette Abschnitte. Nicht geschlossene Sterne bleiben als Text erhalten.
 * Das Ergebnis wird als Text (nicht als HTML) gerendert – Eingaben können kein Markup einschleusen.
 */
export function parseBold(text: string): TextSegment[] {
  const segments: TextSegment[] = [];
  let last = 0;
  for (const match of text.matchAll(BOLD)) {
    const start = match.index ?? 0;
    if (start > last) {
      segments.push({ text: text.slice(last, start), bold: false });
    }
    segments.push({ text: match[1], bold: true });
    last = start + match[0].length;
  }
  if (last < text.length) {
    segments.push({ text: text.slice(last), bold: false });
  }
  return segments;
}

/**
 * Umschließt die Auswahl [start, end) mit `**`. Ohne Auswahl wird ein Platzhalter eingefügt und markiert.
 * Liefert den neuen Text und die neue Auswahl (ohne die Sterne).
 */
export function wrapBold(
  text: string,
  start: number,
  end: number,
  placeholder = 'fetter Text',
): { text: string; start: number; end: number } {
  const selected = text.slice(start, end);
  // Leerzeichen am Rand der Auswahl (z. B. nach Doppelklick) außerhalb der Sterne lassen.
  const inner = selected.trim() || placeholder;
  const before = selected.length - selected.trimStart().length;
  const after = selected.length - selected.trimEnd().length;
  const leading = selected.trim() ? selected.slice(0, before) : '';
  const trailing = selected.trim() ? selected.slice(selected.length - after) : '';
  const replacement = `${leading}**${inner}**${trailing}`;
  const newStart = start + leading.length + 2;
  return {
    text: text.slice(0, start) + replacement + text.slice(end),
    start: newStart,
    end: newStart + inner.length,
  };
}
