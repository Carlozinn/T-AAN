import { ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize, switchMap, tap, timer } from 'rxjs';

import { CuentaCiudadanaService, ReporteCreado, ReportesCiudadanosService, Ubicacion } from '@taan/comun/api';
import { PerfilCiudadano } from '@taan/comun/cuenta/perfil-ciudadano';
import { leerApiError } from '@taan/comun/errores/api-error';
import { marcarErroresDeCampo } from '@taan/comun/errores/errores-formulario';
import { mensajeDeError } from '@taan/comun/errores/mensajes-error';
import { MapaUbicacion } from '@taan/comun/mapa/mapa-ubicacion';
import { ErrorCampo } from '../../../compartido/error-campo';
import { ReenvioVerificacion } from '../../../compartido/reenvio-verificacion';

/** Mismos límites que el backend (RNF-022 punto 4, RNF-017). */
const MAX_FOTOS = 3;
const MAX_BYTES_FOTO = 5 * 1024 * 1024;
const TIPOS_FOTO = ['image/jpeg', 'image/png', 'image/webp'];
const TELEFONO = /^[0-9]{10}$/;
/** Espera tras mover el marcador antes de consultar la cobertura. */
export const ESPERA_COBERTURA_MS = 400;

interface Foto {
  archivo: File;
  vistaPrevia: string;
}

/**
 * Registrar un reporte en una sola pantalla (CU-01, RNF-001): qué pasa, dónde (mapa) y fotos
 * opcionales. Avisa antes de enviar si la ubicación está fuera de cobertura (EX-05) y pide el
 * teléfono en el mismo formulario si la cuenta aún no lo tiene (EX-08).
 */
@Component({
  selector: 'taan-nuevo-reporte',
  imports: [ReactiveFormsModule, RouterLink, MapaUbicacion, ErrorCampo, ReenvioVerificacion],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './nuevo-reporte.html',
  styleUrl: './nuevo-reporte.scss',
})
export class NuevoReporte {
  private readonly reportes = inject(ReportesCiudadanosService);
  private readonly cuenta = inject(CuentaCiudadanaService);
  protected readonly perfil = inject(PerfilCiudadano);

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    descripcion: ['', [Validators.required, Validators.minLength(15), Validators.maxLength(2000)]],
    telefono: ['', Validators.pattern(TELEFONO)],
    campoAuxiliar: [''],
  });
  protected readonly ubicacion = signal<Ubicacion | null>(null);
  protected readonly buscandoUbicacion = signal(false);
  protected readonly avisoUbicacion = signal<string | null>(null);
  protected readonly fotos = signal<Foto[]>([]);
  protected readonly avisoFotos = signal<string | null>(null);
  protected readonly intentoEnvio = signal(false);
  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly correoSinVerificar = signal(false);
  protected readonly creado = signal<ReporteCreado | null>(null);

  /** El backend también puede pedir el teléfono (TELEFONO_REQUERIDO) aunque el perfil no haya cargado. */
  private readonly telefonoPedido = signal(false);
  protected readonly requiereTelefono = computed(() => {
    const perfil = this.perfil.perfil();
    return this.telefonoPedido() || (!!perfil && !perfil.telefono);
  });

  protected readonly cobertura = rxResource({
    params: () => this.ubicacion() ?? undefined,
    stream: ({ params: u }) =>
      timer(ESPERA_COBERTURA_MS).pipe(switchMap(() => this.reportes.verificarCobertura(u.latitud, u.longitud))),
  });
  protected readonly fueraDeCobertura = computed(
    () => this.cobertura.hasValue() && this.cobertura.value().dentroDeCobertura === false,
  );

  constructor() {
    effect(() => {
      const telefono = this.formulario.controls.telefono;
      telefono.setValidators(
        this.requiereTelefono() ? [Validators.required, Validators.pattern(TELEFONO)] : Validators.pattern(TELEFONO),
      );
      telefono.updateValueAndValidity();
    });
    inject(DestroyRef).onDestroy(() => this.liberarFotos());
    this.usarMiUbicacion();
  }

  /** Centra el marcador en la ubicación del dispositivo si el ciudadano da permiso (RF-005). */
  protected usarMiUbicacion(): void {
    if (!navigator.geolocation) {
      this.avisoUbicacion.set('Tu navegador no comparte la ubicación. Marca en el mapa dónde está el problema.');
      return;
    }
    this.buscandoUbicacion.set(true);
    this.avisoUbicacion.set(null);
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        this.buscandoUbicacion.set(false);
        this.ubicacion.set({ latitud: coords.latitude, longitud: coords.longitude });
      },
      () => {
        this.buscandoUbicacion.set(false);
        this.avisoUbicacion.set('No pudimos obtener tu ubicación. Marca en el mapa dónde está el problema.');
      },
      { enableHighAccuracy: true, timeout: 10000 },
    );
  }

  protected agregarFotos(evento: Event): void {
    const entrada = evento.target as HTMLInputElement;
    const elegidas = Array.from(entrada.files ?? []);
    entrada.value = '';
    const validas = elegidas.filter((f) => TIPOS_FOTO.includes(f.type) && f.size <= MAX_BYTES_FOTO);
    const espacio = MAX_FOTOS - this.fotos().length;
    const agregadas = validas.slice(0, espacio).map((archivo) => ({ archivo, vistaPrevia: URL.createObjectURL(archivo) }));
    this.fotos.update((actuales) => [...actuales, ...agregadas]);

    if (validas.length < elegidas.length) {
      this.avisoFotos.set('Algunas fotos no se agregaron: usa JPG, PNG o WebP de hasta 5 MB.');
    } else if (agregadas.length < validas.length) {
      this.avisoFotos.set(`Puedes adjuntar hasta ${MAX_FOTOS} fotos.`);
    } else {
      this.avisoFotos.set(null);
    }
  }

  protected quitarFoto(indice: number): void {
    URL.revokeObjectURL(this.fotos()[indice].vistaPrevia);
    this.fotos.update((actuales) => actuales.filter((_, i) => i !== indice));
    this.avisoFotos.set(null);
  }

  protected enviar(): void {
    this.intentoEnvio.set(true);
    const ubicacion = this.ubicacion();
    if (this.formulario.invalid || !ubicacion || this.fueraDeCobertura()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const { descripcion, telefono, campoAuxiliar } = this.formulario.getRawValue();
    const registrar = this.reportes.crearReporte(
      { descripcion, ubicacion, origen: 'web', campoAuxiliar },
      this.fotos().map((f) => f.archivo),
    );
    const envio = this.requiereTelefono()
      ? this.cuenta.actualizarPerfil({ telefono }).pipe(
          tap((perfil) => {
            this.perfil.actualizar(perfil);
            this.telefonoPedido.set(false);
          }),
          switchMap(() => registrar),
        )
      : registrar;

    this.enviando.set(true);
    this.error.set(null);
    this.correoSinVerificar.set(false);
    envio.pipe(finalize(() => this.enviando.set(false))).subscribe({
      next: (creado) => {
        this.creado.set(creado);
        this.liberarFotos();
      },
      // Lo capturado se conserva siempre (RNF-011, RNF-022).
      error: (e) => {
        const error = leerApiError(e);
        marcarErroresDeCampo(this.formulario, error);
        this.telefonoPedido.set(error.codigo === 'TELEFONO_REQUERIDO');
        this.correoSinVerificar.set(error.codigo === 'CORREO_NO_VERIFICADO');
        this.error.set(mensajeDeError(error));
      },
    });
  }

  protected otroReporte(): void {
    this.creado.set(null);
    this.intentoEnvio.set(false);
    this.formulario.reset();
  }

  private liberarFotos(): void {
    this.fotos().forEach((f) => URL.revokeObjectURL(f.vistaPrevia));
    this.fotos.set([]);
  }
}
