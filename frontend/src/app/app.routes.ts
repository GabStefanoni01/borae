import { Routes } from '@angular/router';
import { authGuard } from './auth.guard';
import { DashboardPage } from './dashboard.page';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./auth.page').then(module => module.AuthPage)
  },
  {
    path: 'app',
    canActivate: [authGuard],
    component: DashboardPage
  },
  {
    path: '**',
    redirectTo: ''
  }
];
