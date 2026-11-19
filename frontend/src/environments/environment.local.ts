import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Ambiente local de desenvolvimento e homologação E2E (`ng serve --configuration local`):
 * conecta o frontend Angular diretamente ao backend Spring Boot em http://localhost:8080.
 */
export const environment = {
  demo: false,
  apiUrl: 'http://localhost:8080'
};

export const environmentInterceptors: HttpInterceptorFn[] = [];
