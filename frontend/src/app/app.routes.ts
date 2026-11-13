import { Routes } from '@angular/router';
import { authGuard } from './auth.guard';
import { DashboardPage } from './dashboard.page';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./landing/landing.page').then(m => m.LandingPage)
  },
  {
    path: 'auth',
    loadComponent: () => import('./auth.page').then(m => m.AuthPage)
  },
  {
    path: 'login',
    redirectTo: 'auth'
  },
  {
    path: 'cadastro',
    redirectTo: 'auth'
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
