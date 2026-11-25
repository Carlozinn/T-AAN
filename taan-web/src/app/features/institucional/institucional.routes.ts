import { Routes } from '@angular/router';

import { rolGuard } from '../../core/auth/rol.guard';

/**
 * Panel institucional (personal de área y coordinación). El guard va en el grupo protegido, no en
 * /institucional, para que /institucional/acceso quede público. Pantallas a crear:
 * - acceso y cambio de contraseña (CU-04)
 * - bandeja: lista con filtros y semáforo (CU-05)
 * - detalle-reporte: validar, rechazar, duplicado, asignar, atender, resolver, redirigir, señalar
 *   (CU-06, CU-07, CU-08, CU-09, CU-17, CU-21, CU-26)
 * - estadisticas: indicadores e informes (CU-10)
 * - configuracion (solo coordinación): áreas, personal, semáforo y plantilla (CU-12, CU-13, CU-22)
 */
export const INSTITUCIONAL_ROUTES: Routes = [
  { path: '', canActivate: [rolGuard('personal_area', 'coordinacion')], children: [] },
];
