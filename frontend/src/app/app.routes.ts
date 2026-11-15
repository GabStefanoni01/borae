import { Routes } from '@angular/router';
import { authGuard } from './auth.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./landing/landing.page').then(m => m.LandingPage)
  },
  {
    path: 'blog',
    loadChildren: () => import('./blog/blog.routes').then(m => m.BLOG_ROUTES)
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
    loadChildren: () => import('./dashboard/dashboard.routes').then(m => m.DASHBOARD_ROUTES)
  },
  {
    // Página pública do restaurante (sem login). O prefixo /r/ precisa coincidir com
    // PUBLIC_RESTAURANT_URL_PREFIX do backend, que gera o QR Code.
    path: 'r/:slug',
    loadComponent: () => import('./public-menu/public-menu.page').then(m => m.PublicMenuPage)
  },
  {
    path: '**',
    redirectTo: ''
  }
];
