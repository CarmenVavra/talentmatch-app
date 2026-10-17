export const MAX_FILE_BYTES = 10 * 1024 * 1024;

/** Clientseitige Prüfung eines Lebenslaufs (der Server prüft zusätzlich Inhalt und Größe). */
export function validateFile(file: File): string | null {
  const isPdf = file.name.toLowerCase().endsWith('.pdf');
  if (!isPdf) {
    return 'Falscher Dateityp – nur PDF-Dateien sind erlaubt.';
  }
  if (file.size > MAX_FILE_BYTES) {
    return 'Die Datei ist größer als 10 MB.';
  }
  return null;
}
