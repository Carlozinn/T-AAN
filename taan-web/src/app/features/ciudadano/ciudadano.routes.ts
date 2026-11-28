import { Routes } from '@angular/router';

import { MarcoCiudadano } from './marco-ciudadano/marco-ciudadano';

/** Portal ciudadano (exige sesión ciudadana). */
export const CIUDADANO_ROUTES: Routes = [
  {
    path: '',
    component: MarcoCiudadano,
    children: [
      {
        path: 'reportar',
        title: 'Reportar un problema · T’AAN',
        loadComponent: () => import('./nuevo-reporte/nuevo-reporte').then((m) => m.NuevoReporte),
      },
      {
        path: 'mis-reportes',
        title: 'Mis reportes · T’AAN',
        loadComponent: () => import('./mis-reportes/mis-reportes').then((m) => m.MisReportes),
      },
      {
        path: 'mis-reportes/:folio',
        title: 'Detalle del reporte · T’AAN',
        loadComponent: () => import('./detalle-reporte/detalle-reporte').then((m) => m.DetalleReporte),
      },
      {
        path: 'perfil',
        title: 'Mi perfil · T’AAN',
        loadComponent: () => import('./perfil/perfil').then((m) => m.Perfil),
      },
    ],
  },
];
