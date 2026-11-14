import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { Rol, SesionService } from './sesion.service';

/**
 * Permite entrar a una sección solo con alguno de los roles indicados.
 * Es una comodidad de la interfaz: la protección real la aplica el backend.
 */
export function rolGuard(...roles: Rol[]): CanActivateFn {
  return () => {
    const rol = inject(SesionService).rol();
    return rol !== null && roles.includes(rol) ? true : inject(Router).createUrlTree(['/acceso']);
  };
}
