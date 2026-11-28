import { formatDate } from '@angular/common';
import { Injectable, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';

import { CuentaCiudadanaService, Perfil } from '../api';

/**
 * Perfil de la cuenta ciudadana, consultado una vez y compartido por las pantallas del portal.
 * Se provee en el componente que enmarca el portal, así que se descarta al cerrar sesión.
 */
@Injectable()
export class PerfilCiudadano {
  private readonly api = inject(CuentaCiudadanaService);
  private readonly recurso = rxResource({ stream: () => this.api.obtenerPerfil() });

  readonly perfil = computed(() => (this.recurso.hasValue() ? this.recurso.value() : undefined));
  readonly suspendida = computed(() => this.perfil()?.estado === 'suspendida');

  /** Reemplaza el perfil con la respuesta de una actualización. */
  actualizar(perfil: Perfil): void {
    this.recurso.set(perfil);
  }
}

/** Texto de la suspensión con motivo y vigencia (CU-04 FA-04, RF-043); null si la cuenta está activa. */
export function describirSuspension(perfil: Perfil | undefined, locale: string): string | null {
  if (perfil?.estado !== 'suspendida') {
    return null;
  }
  const { hasta, motivo } = perfil.suspension ?? {};
  const vigencia = hasta ? `hasta el ${formatDate(hasta, 'longDate', locale)}` : 'por tiempo indefinido';
  return `Tu cuenta está suspendida ${vigencia}.` + (motivo ? ` Motivo: ${motivo}.` : '');
}
