import { Routes } from '@angular/router';

/** Pantallas sin sesión. /verificacion y /recuperacion son los destinos de los enlaces que envía el backend por correo. */
export const PUBLICO_ROUTES: Routes = [
  { path: 'acceso', title: 'Inicia sesión · T’AAN', loadComponent: () => import('./acceso/acceso').then((m) => m.Acceso) },
  { path: 'registro', title: 'Crea tu cuenta · T’AAN', loadComponent: () => import('./registro/registro').then((m) => m.Registro) },
  {
    path: 'verificacion',
    title: 'Confirmación de correo · T’AAN',
    loadComponent: () => import('./verificacion/verificacion').then((m) => m.Verificacion),
  },
  {
    path: 'recuperacion',
    title: 'Recupera tu acceso · T’AAN',
    loadComponent: () => import('./recuperacion/recuperacion').then((m) => m.Recuperacion),
  },
];
