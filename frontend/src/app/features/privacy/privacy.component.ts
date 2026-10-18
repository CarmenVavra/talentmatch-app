import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

import { AuthService } from '../../core/auth/auth.service';
import { ICONS } from '../../shared/icons';
import { LogoComponent } from '../../shared/logo.component';

/**
 * Datenschutzhinweise für Bewerber:innen – öffentlich erreichbar (Registrierung, Login, Portal).
 * Die Fristen entsprechen den Standardwerten in application.yml.
 */
@Component({
  selector: 'app-privacy',
  imports: [RouterLink, LucideAngularModule, LogoComponent],
  templateUrl: './privacy.component.html',
  styleUrl: './privacy.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PrivacyComponent {
  protected readonly auth = inject(AuthService);
  protected readonly icons = ICONS;
}
