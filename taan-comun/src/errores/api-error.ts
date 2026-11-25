import { HttpErrorResponse } from '@angular/common/http';

import { ModelError } from '../api';

/** Esquema Error del contrato más el estado HTTP. Sin respuesta del servidor, el código es SIN_CONEXION. */
export interface ApiError extends ModelError {
  estado: number;
}

/** Acepta el HttpErrorResponse directo o envuelto como «cause» (así lo entrega resource.error()). */
export function leerApiError(error: unknown): ApiError {
  if (error instanceof Error && error.cause instanceof HttpErrorResponse) {
    error = error.cause;
  }
  if (!(error instanceof HttpErrorResponse)) {
    return { estado: 0, codigo: 'ERROR', mensaje: '' };
  }
  const cuerpo = error.error as Partial<ModelError> | null;
  if (cuerpo && typeof cuerpo.codigo === 'string') {
    return { estado: error.status, codigo: cuerpo.codigo, mensaje: cuerpo.mensaje ?? '', detalles: cuerpo.detalles ?? [] };
  }
  return { estado: error.status, codigo: error.status === 0 ? 'SIN_CONEXION' : 'ERROR', mensaje: '' };
}
