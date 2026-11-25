import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, finalize, map, of, shareReplay, tap, throwError } from 'rxjs';

import { AccesoService, CierreSesion, Rol, Sesion } from '../api';

const CLAVE = 'taan.sesion';

/**
 * Sesión del usuario (esquema Sesion del contrato). Para el prototipo se conserva en localStorage;
 * antes de producción conviene evaluar cookies httpOnly para reducir el riesgo de XSS.
 */
@Injectable({ providedIn: 'root' })
export class SesionService {
  private readonly auth = inject(AccesoService);
  private readonly estado = signal<Sesion | null>(leer());
  private renovacion: Observable<Sesion> | null = null;

  readonly sesion = this.estado.asReadonly();
  readonly rol = computed<Rol | null>(() => this.estado()?.rol ?? null);
  readonly autenticado = computed(() => this.estado() !== null);

  iniciar(sesion: Sesion): void {
    localStorage.setItem(CLAVE, JSON.stringify(sesion));
    this.estado.set(sesion);
  }

  /**
   * Renueva el accessToken una sola vez aunque varias peticiones lo pidan a la vez.
   * Si el backend rechaza el refreshToken (4xx), la sesión local se borra.
   */
  renovar(): Observable<Sesion> {
    const refreshToken = this.estado()?.refreshToken;
    if (!refreshToken) {
      return throwError(() => new Error('Sin sesión'));
    }
    this.renovacion ??= this.auth.refrescarSesion({ refreshToken }).pipe(
      tap({
        next: (sesion) => this.iniciar(sesion),
        error: (e) => e instanceof HttpErrorResponse && e.status >= 400 && e.status < 500 && this.borrar(),
      }),
      finalize(() => (this.renovacion = null)),
      shareReplay(1),
    );
    return this.renovacion;
  }

  /** Cierra la sesión en el backend (RF-031) y la borra localmente aunque esa llamada falle. */
  salir(cierre?: CierreSesion): Observable<void> {
    return this.auth.cerrarSesion(cierre).pipe(
      catchError(() => of(null)),
      map(() => this.borrar()),
    );
  }

  private borrar(): void {
    localStorage.removeItem(CLAVE);
    this.estado.set(null);
  }
}

function leer(): Sesion | null {
  try {
    const guardada = localStorage.getItem(CLAVE);
    return guardada ? (JSON.parse(guardada) as Sesion) : null;
  } catch {
    return null;
  }
}
