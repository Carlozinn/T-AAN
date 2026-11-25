import { Routes } from '@angular/router';

import { rolGuard } from './core/auth/rol.guard';

/**
 * Secciones con carga diferida: cada una descarga su código solo al entrar. Las pantallas públicas
 * (acceso, registro, enlaces del correo) van antes que el portal ciudadano, que exige sesión.
 * La raíz redirige explícitamente: sin eso, la sección pública (path '') coincide con «/» sin
 * mostrar ninguna pantalla.
 */
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'mis-reportes' },
  {
    path: 'institucional',
    loadChildren: () => import('./features/institucional/institucional.routes').then((m) => m.INSTITUCIONAL_ROUTES),
  },
  {
    path: 'admin',
    canActivate: [rolGuard('administrador')],
    loadChildren: () => import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
  },
  {
    path: '',
    loadChildren: () => import('./features/publico/publico.routes').then((m) => m.PUBLICO_ROUTES),
  },
  {
    path: '',
    canActivate: [rolGuard('ciudadano')],
    loadChildren: () => import('./features/ciudadano/ciudadano.routes').then((m) => m.CIUDADANO_ROUTES),
  },
  { path: '**', redirectTo: '' },
];
