import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Ambiente padrão (desenvolvimento e produção): usa a API real.
 * O modo demonstração substitui este arquivo por environment.demo.ts (angular.json → configuração "demo").
 */
export const environment = {
  demo: false
};

/** Interceptores extras do ambiente (vazio fora do modo demonstração). */
export const environmentInterceptors: HttpInterceptorFn[] = [];
