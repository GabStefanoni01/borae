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
    path: '**',
    redirectTo: ''
  }
];
