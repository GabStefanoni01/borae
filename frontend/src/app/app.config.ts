import { ApplicationConfig } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { environmentInterceptors } from '../environments/environment';
import { routes } from './app.routes';
import { authInterceptor } from './auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withComponentInputBinding()),
    // O interceptor JWT vem primeiro; interceptores de ambiente (modo demonstração) recebem a requisição já autenticada.
    provideHttpClient(withInterceptors([authInterceptor, ...environmentInterceptors]))
  ]
};
