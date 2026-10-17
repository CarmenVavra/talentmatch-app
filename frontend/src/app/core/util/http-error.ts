import { HttpErrorResponse } from '@angular/common/http';

import { ProblemDetail } from '../models/api.models';

function isProblemDetail(value: unknown): value is ProblemDetail {
  return typeof value === 'object' && value !== null && ('detail' in value || 'title' in value);
}

/** Liefert eine verständliche deutsche Fehlermeldung aus einer HTTP-Antwort. */
export function errorMessage(
  error: unknown,
  fallback = 'Ein unerwarteter Fehler ist aufgetreten.',
): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'Der Server ist nicht erreichbar. Läuft das Backend?';
    }
    if (isProblemDetail(error.error) && error.error.detail) {
      return error.error.detail;
    }
  }
  return fallback;
}

/** Feldfehler aus einer Validierungsantwort (400). */
export function fieldErrors(error: unknown): Record<string, string> {
  if (error instanceof HttpErrorResponse && isProblemDetail(error.error) && error.error.errors) {
    return error.error.errors;
  }
  return {};
}
