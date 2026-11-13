import { Routes } from '@angular/router';
import { DASHBOARD_MODULES } from './modules';
import { RestaurantContext } from './restaurant-context.service';
import { DashboardShellComponent } from './shell/dashboard-shell.component';

const TITLE = (label: string) => `${label} · Painel Boraê`;

/** Módulos planejados: todos usam a página explicativa, sem operações. */
const plannedRoutes: Routes = DASHBOARD_MODULES
  .filter(module => module.status === 'planned')
  .map(module => ({
    path: module.path,
    title: TITLE(module.label),
    data: { moduleId: module.id },
    loadComponent: () => import('./pages/planned-module.page').then(m => m.PlannedModulePage)
  }));

export const DASHBOARD_ROUTES: Routes = [
  {
    path: '',
    component: DashboardShellComponent,
    providers: [RestaurantContext],
    children: [
      { path: '', title: TITLE('Visão geral'), loadComponent: () => import('./pages/overview.page').then(m => m.OverviewPage) },
      { path: 'restaurante', title: TITLE('Meu restaurante'), loadComponent: () => import('./pages/restaurant.page').then(m => m.RestaurantPage) },
      { path: 'equipe', title: TITLE('Equipe'), loadComponent: () => import('./pages/team.page').then(m => m.TeamPage) },
      { path: 'configuracoes', title: TITLE('Configurações'), loadComponent: () => import('./pages/settings.page').then(m => m.SettingsPage) },
      { path: 'novo-restaurante', title: TITLE('Cadastrar restaurante'), loadComponent: () => import('./pages/create-restaurant.page').then(m => m.CreateRestaurantPage) },
      { path: 'convite', title: TITLE('Aceitar convite'), loadComponent: () => import('./pages/accept-invitation.page').then(m => m.AcceptInvitationPage) },
      ...plannedRoutes,
      { path: '**', redirectTo: '' }
    ]
  }
];
