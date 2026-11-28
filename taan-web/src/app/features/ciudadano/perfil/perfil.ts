import { ChangeDetectionStrategy, Component, LOCALE_ID, computed, effect, inject, signal } from '@angular/core';
import { AbstractControl, FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Observable, finalize } from 'rxjs';

import { CuentaCiudadanaService } from '@taan/comun/api';
import { PerfilCiudadano, describirSuspension } from '@taan/comun/cuenta/perfil-ciudadano';
import { leerApiError } from '@taan/comun/errores/api-error';
import { marcarErroresDeCampo } from '@taan/comun/errores/errores-formulario';
import { mensajeDeError } from '@taan/comun/errores/mensajes-error';
import { SesionService } from '@taan/comun/sesion/sesion.service';
import { ErrorCampo } from '../../../compartido/error-campo';

const CONFIRMACION = 'ELIMINAR';

/** Estado de cada sección del perfil: guardando, resultado y error. */
class Accion {
  readonly enviando = signal(false);
  readonly listo = signal(false);
  readonly error = signal<string | null>(null);

  ejecutar<T>(peticion: Observable<T>, formulario: FormGroup, alTerminar: (r: T) => void): void {
    this.enviando.set(true);
    this.listo.set(false);
    this.error.set(null);
    peticion.pipe(finalize(() => this.enviando.set(false))).subscribe({
      next: (respuesta) => {
        this.listo.set(true);
        alTerminar(respuesta);
      },
      error: (e) => {
        const error = leerApiError(e);
        marcarErroresDeCampo(formulario, error);
        this.error.set(mensajeDeError(error));
      },
    });
  }
}

/**
 * Perfil ciudadano (CU-25): corregir nombre, teléfono y avisos por correo, ver el estado de la
 * cuenta, cambiar la contraseña (solo cuentas de correo) y eliminar la cuenta con confirmación.
 */
@Component({
  selector: 'taan-perfil',
  imports: [ReactiveFormsModule, ErrorCampo],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './perfil.html',
  styleUrl: './perfil.scss',
})
export class Perfil {
  private readonly api = inject(CuentaCiudadanaService);
  private readonly sesion = inject(SesionService);
  private readonly router = inject(Router);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly locale = inject(LOCALE_ID);
  protected readonly perfil = inject(PerfilCiudadano);

  protected readonly datos = this.fb.group({
    nombre: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(150)]],
    telefono: ['', [Validators.required, Validators.pattern(/^[0-9]{10}$/)]],
    avisosCorreo: [false],
  });
  protected readonly password = this.fb.group({
    passwordActual: ['', Validators.required],
    nuevaPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
  });
  protected readonly eliminacion = this.fb.group({
    confirmacion: ['', [Validators.required, (c: AbstractControl) => (c.value === CONFIRMACION ? null : { confirmacion: true })]],
  });

  protected readonly guardado = new Accion();
  protected readonly cambioPassword = new Accion();
  protected readonly baja = new Accion();
  protected readonly confirmandoBaja = signal(false);

  protected readonly suspension = computed(() => describirSuspension(this.perfil.perfil(), this.locale));

  constructor() {
    // Llena el formulario cuando llega el perfil, sin pisar lo que el ciudadano ya empezó a editar.
    effect(() => {
      const perfil = this.perfil.perfil();
      if (perfil && this.datos.pristine) {
        this.datos.reset({ nombre: perfil.nombre, telefono: perfil.telefono ?? '', avisosCorreo: perfil.avisosCorreo });
      }
    });
  }

  protected guardar(): void {
    if (this.datos.invalid) {
      this.datos.markAllAsTouched();
      return;
    }
    this.guardado.ejecutar(this.api.actualizarPerfil(this.datos.getRawValue()), this.datos, (perfil) => {
      this.perfil.actualizar(perfil);
      this.datos.markAsPristine();
    });
  }

  protected cambiarPassword(): void {
    if (this.password.invalid) {
      this.password.markAllAsTouched();
      return;
    }
    this.cambioPassword.ejecutar(this.api.cambiarPasswordCiudadano(this.password.getRawValue()), this.password, () =>
      this.password.reset(),
    );
  }

  /** Eliminar la cuenta (RF-012): los reportes se conservan sin datos personales. */
  protected eliminar(): void {
    if (this.eliminacion.invalid) {
      this.eliminacion.markAllAsTouched();
      return;
    }
    this.baja.ejecutar(this.api.eliminarCuenta({ confirmacion: CONFIRMACION }), this.eliminacion, () => {
      this.sesion.descartar();
      this.router.navigate(['/acceso'], { queryParams: { cuentaEliminada: 1 } });
    });
  }
}
