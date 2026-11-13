import { Routes } from '@angular/router';
import { authGuard } from './auth.guard';
import { DashboardPage } from './dashboard.page';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./app').then(module => module.App)
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
