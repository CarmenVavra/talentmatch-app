import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  computed,
  inject,
  signal,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { filter, map } from 'rxjs';

import { AuthService } from '../core/auth/auth.service';
import { JobStore } from '../core/state/job.store';
import { ICONS } from '../shared/icons';
import { LogoComponent } from '../shared/logo.component';

type NavKey = 'jobs' | 'pipeline' | 'candidates';

interface NavItem {
  key: NavKey;
  label: string;
  link: string[];
}

@Component({
  selector: 'app-header',
  imports: [RouterLink, LucideAngularModule, LogoComponent],
  templateUrl: './app-header.component.html',
  styleUrl: './app-header.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:click)': 'onDocumentClick($event)',
    '(document:keydown.escape)': 'menuOpen.set(false)',
  },
})
export class AppHeaderComponent {
  private readonly router = inject(Router);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly jobStore = inject(JobStore);
  protected readonly auth = inject(AuthService);
  protected readonly icons = ICONS;
  protected readonly menuOpen = signal(false);

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map((e) => e.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );

  protected readonly active = computed<NavKey | null>(() => {
    const url = this.url();
    if (/^\/jobs\/\d+\/pipeline/.test(url)) {
      return 'pipeline';
    }
    if (url.startsWith('/jobs')) {
      return 'jobs';
    }
    if (url.startsWith('/candidates') || url.startsWith('/applications')) {
      return 'candidates';
    }
    return null;
  });

  protected readonly navItems = computed<NavItem[]>(() => {
    const pipelineJobId = this.jobStore.pipelineJobId();
    return [
      { key: 'jobs', label: 'Stellen', link: ['/jobs'] },
      {
        key: 'pipeline',
        label: 'Pipeline',
        link: pipelineJobId ? ['/jobs', String(pipelineJobId), 'pipeline'] : ['/jobs'],
      },
      { key: 'candidates', label: 'Bewerber:innen', link: ['/candidates'] },
    ];
  });

  protected readonly roleLabel = computed(() =>
    this.auth.user()?.role === 'HR_MANAGER' ? 'HR-Manager' : 'Bewerber:in',
  );

  constructor() {
    if (this.auth.isHrManager()) {
      this.jobStore.ensureLoaded();
    }
  }

  protected toggleMenu(event: Event): void {
    event.stopPropagation();
    this.menuOpen.update((open) => !open);
  }

  protected logout(): void {
    this.menuOpen.set(false);
    this.auth.logout();
  }

  protected onDocumentClick(event: Event): void {
    if (this.menuOpen() && !this.host.nativeElement.contains(event.target as Node)) {
      this.menuOpen.set(false);
    }
  }
}
