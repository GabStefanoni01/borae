import { Routes } from '@angular/router';

export const BLOG_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./blog.page').then(m => m.BlogPage)
  },
  {
    path: ':slug',
    loadComponent: () => import('./blog-article.page').then(m => m.BlogArticlePage)
  }
];
