import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable, finalize } from 'rxjs';

import { AccesoService } from '@taan/comun/api';
import { leerApiError } from '@taan/comun/errores/api-error';
import { marcarErroresDeCampo } from '@taan/comun/errores/errores-formulario';
import { mensajeDeError } from '@taan/comun/errores/mensajes-error';
import { ErrorCampo } from '../../../compartido/error-campo';

/**
 * Recuperación de contraseña ciudadana (CU-04 FA-01). Sin token se pide el enlace; con el token
 * del correo (/recuperacion?token=…) se escribe la nueva contraseña.
 */
@Component({
  selector: 'taan-recuperacion',
  imports: [ReactiveFormsModule, RouterLink, ErrorCampo],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './recuperacion.html',
})
export class Recuperacion {
  /** Query param «token». */
  readonly token = input<string>();

  private readonly auth = inject(AccesoService);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly solicitud = this.fb.group({ correo: ['', [Validators.required, Validators.email]] });
  protected readonly restablecimiento = this.fb.group({
    nuevaPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
  });
  protected readonly enviando = signal(false);
  protected readonly listo = signal(false);
  protected readonly error = signal<string | null>(null);

  protected solicitar(): void {
    if (this.solicitud.invalid) {
      this.solicitud.markAllAsTouched();
      return;
    }
    this.enviar(this.auth.solicitarRecuperacion(this.solicitud.getRawValue()), this.solicitud);
  }

  protected restablecer(token: string): void {
    if (this.restablecimiento.invalid) {
      this.restablecimiento.markAllAsTouched();
      return;
    }
    this.enviar(this.auth.restablecerPassword({ token, ...this.restablecimiento.getRawValue() }), this.restablecimiento);
  }

  private enviar(peticion: Observable<unknown>, formulario: FormGroup): void {
    this.enviando.set(true);
    this.error.set(null);
    peticion.pipe(finalize(() => this.enviando.set(false))).subscribe({
      next: () => this.listo.set(true),
      error: (e) => {
        const error = leerApiError(e);
        marcarErroresDeCampo(formulario, error);
        this.error.set(mensajeDeError(error));
      },
    });
  }
}
