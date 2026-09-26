import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../auth/auth.service';

/**
 * A "functional interceptor" (Angular 15+), registered in app.config.ts via
 * provideHttpClient(withInterceptors([authInterceptor])). Runs for every outgoing HttpClient
 * request: attaches the JWT if we have one, and if the backend ever responds 401 (token missing,
 * expired, or invalid - see SecurityConfig's authenticationEntryPoint) it logs us out and sends us
 * back to the login page instead of leaving the app stuck showing stale data.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const token = auth.token;
  const authedReq = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(authedReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        auth.logout();
        router.navigate(['/login']);
      }
      return throwError(() => error);
    }),
  );
};
