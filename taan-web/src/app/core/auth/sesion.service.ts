import { Injectable, computed, signal } from '@angular/core';

/** Roles del contrato (TAAN_API.yaml, esquema Rol). */
export type Rol = 'ciudadano' | 'personal_area' | 'coordinacion' | 'administrador';

/** Esquema Sesion del contrato. */
export interface Sesion {
  accessToken: string;
  refreshToken: string;
  expiraEn: number;
  rol: Rol;
  requiereTelefono?: boolean;
  requiereCambioPassword?: boolean;
}

/**
 * Guarda la sesión del usuario. Para el prototipo se conserva en localStorage;
 * antes de producción conviene evaluar cookies httpOnly para reducir el riesgo de XSS.
 */
@Injectable({ providedIn: 'root' })
export class SesionService {
  private readonly clave = 'taan.sesion';
  private readonly estado = signal<Sesion | null>(this.leer());

  readonly sesion = this.estado.asReadonly();
  readonly rol = computed<Rol | null>(() => this.estado()?.rol ?? null);
  readonly autenticado = computed<boolean>(() => this.estado() !== null);

  iniciar(sesion: Sesion): void {
    localStorage.setItem(this.clave, JSON.stringify(sesion));
    this.estado.set(sesion);
  }

  cerrar(): void {
    localStorage.removeItem(this.clave);
    this.estado.set(null);
  }

  private leer(): Sesion | null {
    try {
      const guardada = localStorage.getItem(this.clave);
      return guardada ? (JSON.parse(guardada) as Sesion) : null;
    } catch {
      return null;
    }
  }
}
