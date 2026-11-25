import { Routes } from '@angular/router';

import { MarcoCiudadano } from './marco-ciudadano/marco-ciudadano';

/**
 * Portal ciudadano (exige sesión ciudadana). Pendientes de siguientes etapas:
 * - nuevo-reporte: descripción, mapa y fotografías (CU-01)
 * - perfil: teléfono, avisos y eliminación de cuenta (CU-25)
 */
export const CIUDADANO_ROUTES: Routes = [
  {
    path: '',
    component: MarcoCiudadano,
    children: [
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
    ],
  },
];
