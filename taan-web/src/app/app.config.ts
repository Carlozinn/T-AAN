import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import localeEsMx from '@angular/common/locales/es-MX';
import { ApplicationConfig, LOCALE_ID, inject, provideBrowserGlobalErrorListeners } from '@angular/core';
import { Router, provideRouter, withComponentInputBinding } from '@angular/router';

import { provideApi } from '@taan/comun/api';
import { AL_EXPIRAR_SESION, authInterceptor } from '@taan/comun/sesion/auth.interceptor';
import { environment } from '../environments/environment';
import { rutaDeAcceso } from './core/auth/rutas-acceso';
import { routes } from './app.routes';

registerLocaleData(localeEsMx);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideApi(environment.apiUrl),
    { provide: LOCALE_ID, useValue: 'es-MX' },
    {
      provide: AL_EXPIRAR_SESION,
      useFactory: () => {
        const router = inject(Router);
        return () => router.navigate([rutaDeAcceso(router.url)], { queryParams: { volver: router.url } });
      },
    },
  ],
};
