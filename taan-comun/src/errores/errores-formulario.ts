import { FormGroup } from '@angular/forms';

import { ApiError } from './api-error';

/** Marca en cada control los errores por campo de una respuesta VALIDACION («servidor»: texto del backend). */
export function marcarErroresDeCampo(formulario: FormGroup, error: ApiError): void {
  for (const { campo, problema } of error.detalles ?? []) {
    const control = campo ? formulario.get(campo) : null;
    if (control) {
      control.setErrors({ servidor: problema ?? 'Revisa este dato.' });
      control.markAsTouched();
    }
  }
}
