import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';

import { AccesoService } from '@taan/comun/api';
import { leerApiError } from '@taan/comun/errores/api-error';
import { mensajeDeError } from '@taan/comun/errores/mensajes-error';
import { ReenvioVerificacion } from '../../../compartido/reenvio-verificacion';

/** Destino del enlace del correo de verificación (/verificacion?token=…, CU-24). */
@Component({
  selector: 'taan-verificacion',
  imports: [RouterLink, ReenvioVerificacion],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main class="contenedor">
      <h1>Confirmación de correo</h1>
      <div class="tarjeta">
        @if (!token()) {
          <p>Abre el enlace que te enviamos por correo o pide uno nuevo.</p>
          <taan-reenvio-verificacion />
        } @else if (verificacion.isLoading()) {
          <p role="status">Confirmando tu correo…</p>
        } @else if (error(); as mensaje) {
          <p class="aviso-error" role="alert">{{ mensaje }}</p>
          <taan-reenvio-verificacion />
        } @else {
          <p class="aviso-exito" role="status">Tu correo quedó confirmado. Ya puedes iniciar sesión y enviar reportes.</p>
          <a class="boton" routerLink="/acceso">Iniciar sesión</a>
        }
      </div>
    </main>
  `,
})
export class Verificacion {
  /** Query param «token». */
  readonly token = input<string>();

  private readonly auth = inject(AccesoService);
  protected readonly verificacion = rxResource({
    params: () => this.token() || undefined,
    stream: ({ params: token }) => this.auth.verificarCorreo({ token }),
  });
  protected readonly error = computed(() => {
    const error = this.verificacion.error();
    return error ? mensajeDeError(leerApiError(error)) : null;
  });
}
