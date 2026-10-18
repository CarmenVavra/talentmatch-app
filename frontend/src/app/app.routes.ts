import { inject } from '@angular/core';
import { Routes } from '@angular/router';

import { applicantGuard, authGuard, guestGuard, hrManagerGuard } from './core/auth/auth.guards';
import { AuthService } from './core/auth/auth.service';
import { ShellComponent } from './layout/shell.component';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    title: 'Anmelden · TalentMatch',
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    title: 'Registrieren · TalentMatch',
    loadComponent: () =>
      import('./features/auth/register.component').then((m) => m.RegisterComponent),
  },
  {
    // Antwortseite der Evidenz-Anfrage – ohne Anmeldung, Zugang über den persönlichen Link
    path: 'evidenz/:token',
    title: 'Evidenz · TalentMatch',
    loadComponent: () =>
      import('./features/talent-pool/talent-pool-answer.component').then(
        (m) => m.TalentPoolAnswerComponent,
      ),
  },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      // Startseite je Rolle
      { path: '', pathMatch: 'full', redirectTo: () => inject(AuthService).homeUrl() },
      {
        path: 'account/password',
        title: 'Passwort ändern · TalentMatch',
        loadComponent: () =>
          import('./features/auth/change-password.component').then(
            (m) => m.ChangePasswordComponent,
          ),
      },
      {
        path: 'portal',
        canActivateChild: [applicantGuard],
        children: [
          { path: '', pathMatch: 'full', redirectTo: 'jobs' },
          {
            path: 'jobs',
            title: 'Offene Stellen · TalentMatch',
            loadComponent: () =>
              import('./features/portal/portal-jobs.component').then((m) => m.PortalJobsComponent),
          },
          {
            path: 'jobs/:jobId',
            title: 'Stellenbeschreibung · TalentMatch',
            loadComponent: () =>
              import('./features/portal/portal-job-detail.component').then(
                (m) => m.PortalJobDetailComponent,
              ),
          },
          {
            path: 'applications',
            title: 'Meine Bewerbungen · TalentMatch',
            loadComponent: () =>
              import('./features/portal/my-applications.component').then(
                (m) => m.MyApplicationsComponent,
              ),
          },
        ],
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
            path: 'jobs/:jobId/edit',
            title: 'Stelle bearbeiten · TalentMatch',
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
            path: 'talent-pool',
            title: 'Evidenz · TalentMatch',
            loadComponent: () =>
              import('./features/talent-pool/talent-pool.component').then(
                (m) => m.TalentPoolComponent,
              ),
          },
          {
            path: 'outbox',
            title: 'Postausgang · TalentMatch',
            loadComponent: () =>
              import('./features/outbox/outbox.component').then((m) => m.OutboxComponent),
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
  { path: '**', redirectTo: '' },
];
