import { CanDeactivateFn, Routes } from '@angular/router';
import { DASHBOARD_MODULES } from './modules';
import { RestaurantContext } from './restaurant-context.service';
import { DashboardShellComponent } from './shell/dashboard-shell.component';
import { ToastService } from './ui/feedback';

const TITLE = (label: string) => `${label} · Painel Boraê`;

/** Confirma antes de sair do formulário de produto com alterações não salvas. */
const leaveProductEditor: CanDeactivateFn<{ canLeave(): boolean | Promise<boolean> }> = component => component.canLeave();

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
    providers: [RestaurantContext, ToastService],
    children: [
      { path: '', title: TITLE('Visão geral'), loadComponent: () => import('./pages/overview.page').then(m => m.OverviewPage) },
      {
        path: 'restaurante',
        loadComponent: () => import('./pages/restaurant.page').then(m => m.RestaurantPage),
        children: [
          { path: '', title: TITLE('Meu restaurante'), loadComponent: () => import('./profile/profile-form.page').then(m => m.ProfileFormPage) },
          { path: 'horarios', title: TITLE('Horários'), loadComponent: () => import('./profile/business-hours.page').then(m => m.BusinessHoursPage) },
          { path: 'imagens', title: TITLE('Imagens'), loadComponent: () => import('./profile/media.page').then(m => m.MediaPage) }
        ]
      },
      {
        path: 'cardapios',
        children: [
          { path: '', title: TITLE('Cardápios'), loadComponent: () => import('./catalog/menus.page').then(m => m.MenusPage) },
          { path: ':menuId', title: TITLE('Cardápio'), loadComponent: () => import('./catalog/menu-editor.page').then(m => m.MenuEditorPage) },
          { path: ':menuId/produtos/novo', title: TITLE('Novo produto'), canDeactivate: [leaveProductEditor], loadComponent: () => import('./catalog/product-editor.page').then(m => m.ProductEditorPage) },
          { path: ':menuId/produtos/:productId', title: TITLE('Editar produto'), canDeactivate: [leaveProductEditor], loadComponent: () => import('./catalog/product-editor.page').then(m => m.ProductEditorPage) }
        ]
      },
      // Categorias e produtos são organizados dentro de cada cardápio.
      { path: 'categorias', redirectTo: 'cardapios' },
      { path: 'produtos', redirectTo: 'cardapios' },
      { path: 'qr-code', title: TITLE('Página pública e QR Code'), loadComponent: () => import('./catalog/publication.page').then(m => m.PublicationPage) },
      { path: 'equipe', title: TITLE('Equipe'), loadComponent: () => import('./pages/team.page').then(m => m.TeamPage) },
      { path: 'configuracoes', title: TITLE('Configurações'), loadComponent: () => import('./pages/settings.page').then(m => m.SettingsPage) },
      { path: 'novo-restaurante', title: TITLE('Cadastrar restaurante'), loadComponent: () => import('./pages/create-restaurant.page').then(m => m.CreateRestaurantPage) },
      { path: 'convite', title: TITLE('Aceitar convite'), loadComponent: () => import('./pages/accept-invitation.page').then(m => m.AcceptInvitationPage) },
      ...plannedRoutes,
      { path: '**', redirectTo: '' }
    ]
  }
];
