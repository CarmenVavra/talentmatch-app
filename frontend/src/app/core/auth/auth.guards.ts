import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from './auth.service';

/** Nur angemeldete Benutzer:innen. */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  if (auth.isAuthenticated()) {
    return true;
  }
  return inject(Router).createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

/** Alle Recruiting-Views sind der Rolle HR-Manager vorbehalten. */
export const hrManagerGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.hasRole('HR_MANAGER') ? true : inject(Router).createUrlTree(['/forbidden']);
};

/** Login-Seite nur ohne aktive Sitzung. */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isAuthenticated() ? inject(Router).createUrlTree(['/jobs']) : true;
};
