import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AccesoService } from '@taan/comun/api';
import { leerApiError } from '@taan/comun/errores/api-error';
import { marcarErroresDeCampo } from '@taan/comun/errores/errores-formulario';
import { mensajeDeError } from '@taan/comun/errores/mensajes-error';
import { SesionService } from '@taan/comun/sesion/sesion.service';
import { ErrorCampo } from '../../../compartido/error-campo';
import { destinoSeguro } from '../../../core/auth/rutas-acceso';

/** Inicio de sesión ciudadano con correo (CU-04). Al entrar vuelve a la página que se pidió, p. ej. un enlace del correo. */
@Component({
  selector: 'taan-acceso',
  imports: [ReactiveFormsModule, RouterLink, ErrorCampo],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './acceso.html',
})
export class Acceso {
  /** Query param «volver». */
  readonly volver = input<string>();
  /** Query param que llega después de eliminar la cuenta. */
  readonly cuentaEliminada = input<string>();

  private readonly auth = inject(AccesoService);
  private readonly sesion = inject(SesionService);
  private readonly router = inject(Router);

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    correo: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });
  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected entrar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    this.auth
      .iniciarSesionCiudadano(this.formulario.getRawValue())
      .pipe(finalize(() => this.enviando.set(false)))
      .subscribe({
        next: (sesion) => {
          this.sesion.iniciar(sesion);
          this.router.navigateByUrl(destinoSeguro(this.volver(), '/reportar'));
        },
        error: (e) => {
          const error = leerApiError(e);
          marcarErroresDeCampo(this.formulario, error);
          this.error.set(mensajeDeError(error));
        },
      });
  }
}
