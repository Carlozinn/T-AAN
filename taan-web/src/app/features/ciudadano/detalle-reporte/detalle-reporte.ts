import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable, finalize } from 'rxjs';

import { ImgProtegida } from '@taan/comun/archivos/img-protegida';
import { CanalExterno, MotivoRechazo, ReportesCiudadanosService } from '@taan/comun/api';
import { leerApiError } from '@taan/comun/errores/api-error';
import { marcarErroresDeCampo } from '@taan/comun/errores/errores-formulario';
import { mensajeDeError } from '@taan/comun/errores/mensajes-error';
import { ErrorCampo } from '../../../compartido/error-campo';
import { Historial } from '../historial/historial';

const MOTIVOS_RECHAZO: Record<MotivoRechazo, string> = {
  no_corresponde: 'El reporte no corresponde a los servicios de esta institución.',
  informacion_insuficiente: 'La información no fue suficiente para atenderlo.',
  ya_atendido: 'El problema ya había sido atendido.',
  no_servicio_publico: 'El problema no corresponde a un servicio público.',
};

/**
 * Detalle de un reporte propio (CU-02, CU-03, CU-23). Es el destino de los enlaces que el backend
 * envía por correo (/mis-reportes/{folio}). Si está resuelto, el ciudadano confirma o lo reabre una vez.
 */
@Component({
  selector: 'taan-detalle-reporte',
  imports: [RouterLink, DatePipe, ReactiveFormsModule, ImgProtegida, Historial, ErrorCampo],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './detalle-reporte.html',
  styleUrl: './detalle-reporte.scss',
})
export class DetalleReporte {
  /** Parámetro de ruta :folio. */
  readonly folio = input.required<string>();

  private readonly api = inject(ReportesCiudadanosService);
  protected readonly reporte = rxResource({
    params: () => this.folio(),
    stream: ({ params: folio }) => this.api.obtenerMiReporte(folio),
  });
  protected readonly errorCarga = computed(() => {
    const error = this.reporte.error();
    return error ? mensajeDeError(leerApiError(error)) : null;
  });
  protected readonly puedeResponder = computed(() => {
    const reporte = this.reporte.hasValue() ? this.reporte.value() : undefined;
    return reporte?.estado === 'resuelto' && !!reporte.reapertura?.permitida;
  });

  protected readonly reabriendo = signal(false);
  protected readonly enviando = signal(false);
  protected readonly confirmado = signal(false);
  protected readonly errorAccion = signal<string | null>(null);
  protected readonly reapertura = new FormGroup({
    motivo: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(10), Validators.maxLength(1000)],
    }),
  });

  protected motivoRechazo(motivo: MotivoRechazo | undefined): string {
    return motivo ? MOTIVOS_RECHAZO[motivo] : '';
  }

  /** Enlace para contactar un canal oficial; los sitios solo con http(s). */
  protected enlaceCanal(canal: CanalExterno): string | null {
    switch (canal.tipo) {
      case 'telefono':
        return `tel:${canal.valor}`;
      case 'correo':
        return `mailto:${canal.valor}`;
      case 'sitio_web':
        return /^https?:\/\//i.test(canal.valor) ? canal.valor : null;
      default:
        return null;
    }
  }

  protected confirmar(folio: string): void {
    this.ejecutar(this.api.confirmarResolucion(folio), () => {
      this.confirmado.set(true);
      this.reporte.reload();
    });
  }

  protected reabrir(folio: string): void {
    if (this.reapertura.invalid) {
      this.reapertura.markAllAsTouched();
      return;
    }
    this.ejecutar(this.api.reabrirReporte(folio, this.reapertura.getRawValue()), (detalle) => {
      this.reabriendo.set(false);
      this.reporte.set(detalle);
    });
  }

  /** Si el estado cambió mientras tanto (409/422), se recarga para mostrar la situación actual. */
  private ejecutar<T>(peticion: Observable<T>, alTerminar: (respuesta: T) => void): void {
    this.enviando.set(true);
    this.errorAccion.set(null);
    peticion.pipe(finalize(() => this.enviando.set(false))).subscribe({
      next: alTerminar,
      error: (e) => {
        const error = leerApiError(e);
        marcarErroresDeCampo(this.reapertura, error);
        this.errorAccion.set(mensajeDeError(error));
        if (error.estado === 409 || error.estado === 422) {
          this.reporte.reload();
        }
      },
    });
  }
}
