import { Routes } from '@angular/router';

import { rolGuard } from './core/auth/rol.guard';

/**
 * Tres secciones con carga diferida: cada una descarga su código solo al entrar.
 * La ruta /acceso (inicio de sesión) se agrega en las rutas de ciudadano.
 */
export const routes: Routes = [
  {
    path: 'institucional',
    canActivate: [rolGuard('personal_area', 'coordinacion')],
    loadChildren: () => import('./features/institucional/institucional.routes').then((m) => m.INSTITUCIONAL_ROUTES),
  },
  {
    path: 'admin',
    canActivate: [rolGuard('administrador')],
    loadChildren: () => import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
  },
  {
    path: '',
    loadChildren: () => import('./features/ciudadano/ciudadano.routes').then((m) => m.CIUDADANO_ROUTES),
  },
  { path: '**', redirectTo: '' },
];
