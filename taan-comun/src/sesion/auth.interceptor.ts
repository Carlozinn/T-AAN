import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { InjectionToken, inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';

import { BASE_PATH } from '../api';
import { SesionService } from './sesion.service';

/** Qué hacer cuando la sesión terminó y no pudo renovarse (la web y la app navegan distinto). */
export const AL_EXPIRAR_SESION = new InjectionToken<() => void>('AL_EXPIRAR_SESION', {
  providedIn: 'root',
  factory: () => () => undefined,
});

/**
 * Agrega «Authorization: Bearer» solo a las peticiones al backend de T'AAN. Ante un 401 renueva
 * la sesión con el refreshToken y reintenta una vez; si no se puede renovar, avisa con AL_EXPIRAR_SESION.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const base = inject(BASE_PATH, { optional: true });
  if (!base || !req.url.startsWith(base)) {
    return next(req);
  }
  const sesion = inject(SesionService);
  const alExpirar = inject(AL_EXPIRAR_SESION);
  const conToken = (r: HttpRequest<unknown>) => {
    const token = sesion.sesion()?.accessToken;
    return token ? r.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : r;
  };

  return next(conToken(req)).pipe(
    catchError((error) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401 || !sesion.autenticado()) {
        return throwError(() => error);
      }
      return sesion.renovar().pipe(
        catchError(() => {
          if (!sesion.autenticado()) {
            alExpirar();
          }
          return throwError(() => error);
        }),
        switchMap(() => next(conToken(req))),
      );
    }),
  );
};
