import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, ValidationErrors } from '@angular/forms';
import { map, startWith, switchMap } from 'rxjs';

/** Muestra el primer error de un control ya tocado, en palabras que orientan la corrección (RNF-002). */
@Component({
  selector: 'taan-error-campo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (mensaje(); as texto) {
      <span class="error-campo">{{ texto }}</span>
    }
  `,
})
export class ErrorCampo {
  readonly control = input.required<AbstractControl>();

  private readonly estado = toSignal(
    toObservable(this.control).pipe(
      switchMap((c) => c.events.pipe(startWith(null), map(() => ({ errores: c.errors, tocado: c.touched })))),
    ),
  );

  protected readonly mensaje = computed(() => {
    const estado = this.estado();
    return estado?.tocado && estado.errores ? textoDe(estado.errores) : null;
  });
}

function textoDe(errores: ValidationErrors): string {
  if (errores['servidor']) return errores['servidor'];
  if (errores['required']) return 'Este dato es obligatorio.';
  if (errores['email']) return 'Escribe un correo válido, por ejemplo nombre@correo.com.';
  if (errores['minlength']) return `Escribe al menos ${errores['minlength'].requiredLength} caracteres.`;
  if (errores['maxlength']) return `Escribe como máximo ${errores['maxlength'].requiredLength} caracteres.`;
  return 'Revisa este dato.';
}
