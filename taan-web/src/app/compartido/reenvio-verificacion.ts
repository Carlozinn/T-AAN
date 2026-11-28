import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { AccesoService } from '@taan/comun/api';
import { leerApiError } from '@taan/comun/errores/api-error';
import { mensajeDeError } from '@taan/comun/errores/mensajes-error';
import { ErrorCampo } from './error-campo';

/** Reenvía el enlace de verificación (CU-24). Si ya se conoce el correo, basta un botón. */
@Component({
  selector: 'taan-reenvio-verificacion',
  imports: [ReactiveFormsModule, ErrorCampo],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (enviado()) {
      <p class="aviso-exito" role="status">Te enviamos un nuevo enlace. Revisa tu bandeja de entrada y la de correo no deseado.</p>
    }
    @if (error(); as mensaje) {
      <p class="aviso-error" role="alert">{{ mensaje }}</p>
    }
    <form [formGroup]="formulario" (ngSubmit)="reenviar()" novalidate>
      @if (!correo()) {
        <div class="campo">
          <label for="correo-reenvio">Correo electrónico</label>
          <input
            id="correo-reenvio"
            type="email"
            formControlName="correo"
            autocomplete="email"
            aria-describedby="correo-reenvio-error"
            [attr.aria-invalid]="formulario.controls.correo.touched && formulario.controls.correo.invalid"
          />
          <taan-error-campo id="correo-reenvio-error" [control]="formulario.controls.correo" />
        </div>
      }
      <div>
        <button class="boton-secundario" type="submit" [disabled]="enviando()">Enviar un nuevo enlace</button>
      </div>
    </form>
  `,
})
export class ReenvioVerificacion {
  readonly correo = input<string>();

  private readonly auth = inject(AccesoService);
  protected readonly formulario = inject(NonNullableFormBuilder).group({
    correo: ['', [Validators.required, Validators.email]],
  });
  protected readonly enviando = signal(false);
  protected readonly enviado = signal(false);
  protected readonly error = signal<string | null>(null);

  protected reenviar(): void {
    const correo = this.correo() ?? this.formulario.controls.correo.value;
    if (!this.correo() && this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.enviado.set(false);
    this.error.set(null);
    this.auth
      .reenviarVerificacion({ correo })
      .pipe(finalize(() => this.enviando.set(false)))
      .subscribe({
        next: () => this.enviado.set(true),
        error: (e) => this.error.set(mensajeDeError(leerApiError(e))),
      });
  }
}
