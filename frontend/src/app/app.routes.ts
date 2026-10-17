import { Routes } from '@angular/router';

import { authGuard, guestGuard, hrManagerGuard } from './core/auth/auth.guards';
import { ShellComponent } from './layout/shell.component';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    title: 'Anmelden · TalentMatch',
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent),
  },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'jobs' },
      {
        path: 'account/password',
        title: 'Passwort ändern · TalentMatch',
        loadComponent: () =>
          import('./features/auth/change-password.component').then(
            (m) => m.ChangePasswordComponent,
          ),
      },
      {
        path: 'forbidden',
        title: 'Kein Zugriff · TalentMatch',
        loadComponent: () =>
          import('./features/auth/forbidden.component').then((m) => m.ForbiddenComponent),
      },
      {
        path: '',
        canActivateChild: [hrManagerGuard],
        children: [
          {
            path: 'jobs',
            title: 'Offene Stellen · TalentMatch',
            loadComponent: () =>
              import('./features/jobs/job-dashboard.component').then(
                (m) => m.JobDashboardComponent,
              ),
          },
          {
            path: 'jobs/new',
            title: 'Neue Stelle · TalentMatch',
            loadComponent: () =>
              import('./features/jobs/job-create.component').then((m) => m.JobCreateComponent),
          },
          {
            path: 'jobs/:jobId/pipeline',
            title: 'Pipeline · TalentMatch',
            loadComponent: () =>
              import('./features/pipeline/pipeline-board.component').then(
                (m) => m.PipelineBoardComponent,
              ),
          },
          {
            path: 'applications/:id',
            title: 'Bewerber:in · TalentMatch',
            loadComponent: () =>
              import('./features/application-detail/application-detail.component').then(
                (m) => m.ApplicationDetailComponent,
              ),
          },
          {
            path: 'candidates',
            title: 'Bewerber:innen · TalentMatch',
            loadComponent: () =>
              import('./features/candidates/candidate-list.component').then(
                (m) => m.CandidateListComponent,
              ),
          },
        ],
      },
    ],
  },
  { path: '**', redirectTo: 'jobs' },
];
