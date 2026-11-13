import { HttpInterceptorFn } from '@angular/common/http';
import { demoApiInterceptor } from '../app/demo/demo-api.interceptor';

/**
 * MODO DEMONSTRAÇÃO (`npm run start:demo`): a API é simulada localmente, com dados fictícios.
 * Credenciais de teste: src/app/demo/demo-data.ts.
 */
export const environment = {
  demo: true
};

export const environmentInterceptors: HttpInterceptorFn[] = [demoApiInterceptor];
