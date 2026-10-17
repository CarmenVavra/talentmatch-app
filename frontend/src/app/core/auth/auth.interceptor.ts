import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { AuthService } from './auth.service';

/**
 * Hängt das JWT an alle API-Aufrufe und beendet die Sitzung bei 401 (abgelaufenes/ungültiges Token).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token();
  const isApi = req.url.startsWith('/api/');
  const isLogin = req.url === '/api/auth/login';

  const request =
    token && isApi && !isLogin
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;

  return next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && isApi && !isLogin) {
        auth.logout();
      }
      return throwError(() => error);
    }),
  );
};
