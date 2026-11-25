import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { Rol } from '@taan/comun/api';
import { SesionService } from '@taan/comun/sesion/sesion.service';
import { rutaDeAcceso } from './rutas-acceso';

/**
 * Permite entrar a una sección solo con alguno de los roles indicados; si no, lleva al inicio de
 * sesión que corresponde y recuerda a dónde volver. Es una comodidad de la interfaz: la protección real la aplica el backend.
 */
export function rolGuard(...roles: Rol[]): CanActivateFn {
  return (_ruta, estado) => {
    const rol = inject(SesionService).rol();
    if (rol !== null && roles.includes(rol)) {
      return true;
    }
    return inject(Router).createUrlTree([rutaDeAcceso(estado.url)], { queryParams: { volver: estado.url } });
  };
}
