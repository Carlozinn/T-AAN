import { ChangeDetectionStrategy, Component, inject, signal, viewChild } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AccesoService } from '@taan/comun/api';
import { leerApiError } from '@taan/comun/errores/api-error';
import { marcarErroresDeCampo } from '@taan/comun/errores/errores-formulario';
import { mensajeDeError } from '@taan/comun/errores/mensajes-error';
import { Captcha } from '../../../compartido/captcha';
import { ErrorCampo } from '../../../compartido/error-campo';
import { ReenvioVerificacion } from '../../../compartido/reenvio-verificacion';

/**
 * Registro ciudadano con correo (CU-24): nombre, correo, contraseña, aviso de privacidad y
 * verificación antiabuso (RNF-022). La cuenta podrá reportar después de confirmar el enlace.
 */
@Component({
  selector: 'taan-registro',
  imports: [ReactiveFormsModule, RouterLink, ErrorCampo, Captcha, ReenvioVerificacion],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './registro.html',
})
export class Registro {
  private readonly auth = inject(AccesoService);
  private readonly captcha = viewChild.required(Captcha);

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    nombre: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(150)]],
    correo: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
    aceptaAvisoPrivacidad: [false, Validators.requiredTrue],
    campoAuxiliar: [''],
  });
  protected readonly captchaToken = signal<string | null>(null);
  protected readonly faltaCaptcha = signal(false);
  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);
  /** Correo al que se envió el enlace; con él se muestra la confirmación. */
  protected readonly enviadoA = signal<string | null>(null);

  protected registrar(): void {
    const captchaToken = this.captchaToken();
    this.faltaCaptcha.set(!captchaToken);
    if (this.formulario.invalid || !captchaToken) {
      this.formulario.markAllAsTouched();
      return;
    }
    const datos = this.formulario.getRawValue();
    this.enviando.set(true);
    this.error.set(null);
    this.auth
      .registrarCiudadanoCorreo({ ...datos, captchaToken })
      .pipe(finalize(() => this.enviando.set(false)))
      .subscribe({
        next: () => this.enviadoA.set(datos.correo),
        error: (e) => {
          const error = leerApiError(e);
          marcarErroresDeCampo(this.formulario, error);
          this.error.set(mensajeDeError(error));
          this.captcha().reiniciar();
        },
      });
  }
}
