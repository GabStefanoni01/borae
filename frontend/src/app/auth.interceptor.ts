import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { API_URL, AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const isApiRequest = request.url.startsWith(API_URL);
  const isAuthenticationRequest = request.url.includes('/api/v1/auth/');

  const token = isApiRequest && !isAuthenticationRequest ? auth.accessToken : null;
  const authenticatedRequest = token
    ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : request;

  return next(authenticatedRequest).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        isApiRequest &&
        !isAuthenticationRequest
      ) {
        auth.logout();
        void router.navigateByUrl('/');
      }

      return throwError(() => error);
    })
  );
};
