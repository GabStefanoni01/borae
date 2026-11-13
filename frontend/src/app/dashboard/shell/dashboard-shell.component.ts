import {
  ChangeDetectionStrategy, Component, HostListener, OnInit, ViewEncapsulation, computed, inject, signal
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../auth.service';
import { IconComponent } from '../../shared/icon.component';
import { BrandLogoComponent } from '../../shared/brand-logo.component';
import { ThemeToggleComponent } from '../../shared/theme-toggle.component';
import { DASHBOARD_MODULES, MODULE_GROUPS, moduleByPath } from '../modules';
import { RestaurantContext } from '../restaurant-context.service';
import { RoleBadgeComponent, StateComponent } from '../ui/ui.components';
import { RestaurantSwitcherComponent } from './restaurant-switcher.component';

const EXTRA_PAGES: Record<string, string> = {
  'novo-restaurante': 'Cadastrar restaurante',
  convite: 'Aceitar convite'
};

/**
 * Layout autenticado do painel: sidebar (fixa no desktop, gaveta no mobile),
 * topbar contextual e área de conteúdo. Os estilos do painel (tokens + componentes .d-*)
 * são globais ao painel (ViewEncapsulation.None), mas todos prefixados por .dash / .d-.
 */
@Component({
  selector: 'bo-dashboard-shell',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive, IconComponent, BrandLogoComponent, ThemeToggleComponent,
    RestaurantSwitcherComponent, RoleBadgeComponent, StateComponent
  ],
  templateUrl: './dashboard-shell.component.html',
  styleUrls: ['../styles/tokens.css', '../styles/layout.css', '../styles/ui.css', '../styles/ui-menus.css', '../styles/ui-forms.css']
})
export class DashboardShellComponent implements OnInit {
  protected readonly ctx = inject(RestaurantContext);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly groups = MODULE_GROUPS.map(group => ({
    name: group,
    modules: DASHBOARD_MODULES.filter(module => module.group === group)
  }));

  protected readonly navOpen = signal(false);
  protected readonly accountOpen = signal(false);
  protected readonly demo = environment.demo;

  private readonly url = toSignal(
    this.router.events.pipe(filter(e => e instanceof NavigationEnd), map(() => this.router.url)),
    { initialValue: this.router.url }
  );

  /** Breadcrumb a partir da rota atual e do catálogo de módulos. */
  protected readonly crumb = computed(() => {
    const segment = this.url().split(/[?#]/)[0].replace(/^\/app\/?/, '').split('/')[0];
    return moduleByPath(segment)?.label ?? EXTRA_PAGES[segment] ?? 'Início';
  });

  protected readonly firstName = computed(() => this.ctx.user()?.fullName.split(' ')[0] ?? '');
  protected readonly userInitial = computed(() => (this.ctx.user()?.fullName ?? '?').charAt(0).toUpperCase());

  ngOnInit(): void {
    // Sempre recarrega: o injetor da rota sobrevive entre sessões diferentes.
    this.ctx.clear();
    this.ctx.load();
  }

  protected link(path: string): string {
    return path ? `/app/${path}` : '/app';
  }

  protected toggleNav(): void {
    this.navOpen.update(v => !v);
    this.lockScroll(this.navOpen());
  }

  protected closeNav(): void {
    this.navOpen.set(false);
    this.lockScroll(false);
  }

  protected logout(): void {
    this.ctx.clear();
    this.auth.logout();
    void this.router.navigateByUrl('/auth');
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.closeNav();
    this.accountOpen.set(false);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (this.accountOpen() && !target.closest('.d-account')) this.accountOpen.set(false);
  }

  private lockScroll(lock: boolean): void {
    document.body.style.overflow = lock ? 'hidden' : '';
  }
}
