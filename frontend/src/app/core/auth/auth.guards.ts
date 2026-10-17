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

/** Die Recruiting-Views sind der Rolle HR-Manager vorbehalten; Bewerber:innen landen im Portal. */
export const hrManagerGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.hasRole('HR_MANAGER') ? true : inject(Router).createUrlTree(['/portal']);
};

/** Das Bewerber-Portal ist der Rolle Bewerber:in vorbehalten; HR landet in der Stellenübersicht. */
export const applicantGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.hasRole('APPLICANT') ? true : inject(Router).createUrlTree(['/jobs']);
};

/** Login und Registrierung nur ohne aktive Sitzung. */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isAuthenticated() ? inject(Router).parseUrl(auth.homeUrl()) : true;
};
