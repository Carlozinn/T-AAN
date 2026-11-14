import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { environment } from '../../../environments/environment';
import { SesionService } from './sesion.service';

/** Agrega «Authorization: Bearer <token>» solo a las peticiones dirigidas al backend de T'AAN. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(SesionService).sesion()?.accessToken;
  if (!token || !req.url.startsWith(environment.apiUrl)) {
    return next(req);
  }
  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
