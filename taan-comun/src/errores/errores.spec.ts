import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup } from '@angular/forms';

import { leerApiError } from './api-error';
import { marcarErroresDeCampo } from './errores-formulario';
import { mensajeDeError } from './mensajes-error';

const respuesta = (status: number, error: unknown) => new HttpErrorResponse({ status, error });

describe('leerApiError', () => {
  it('lee el esquema Error del contrato', () => {
    const error = leerApiError(respuesta(409, { codigo: 'YA_REABIERTO', mensaje: 'Ya se reabrió.' }));
    expect(error).toEqual({ estado: 409, codigo: 'YA_REABIERTO', mensaje: 'Ya se reabrió.', detalles: [] });
  });

  it('sin respuesta del servidor devuelve SIN_CONEXION', () => {
    expect(leerApiError(respuesta(0, null)).codigo).toBe('SIN_CONEXION');
  });

  it('acepta el error envuelto como cause, como lo entrega resource.error()', () => {
    const envuelto = new Error('envuelto', { cause: respuesta(404, { codigo: 'NO_ENCONTRADO', mensaje: '' }) });
    expect(leerApiError(envuelto)).toMatchObject({ estado: 404, codigo: 'NO_ENCONTRADO' });
  });
});

describe('mensajeDeError', () => {
  it('usa el mensaje propio del código', () => {
    expect(mensajeDeError({ estado: 409, codigo: 'YA_CONFIRMADO', mensaje: 'x' })).toContain('Ya confirmaste');
  });

  it('el límite excedido recuerda el 911 (RNF-022)', () => {
    expect(mensajeDeError({ estado: 429, codigo: 'LIMITE_EXCEDIDO', mensaje: '' })).toContain('911');
  });

  it('sin mensaje propio usa el del backend en errores 4xx', () => {
    expect(mensajeDeError({ estado: 400, codigo: 'CREDENCIALES_INVALIDAS', mensaje: 'Revisa tu correo y contraseña.' })).toBe(
      'Revisa tu correo y contraseña.',
    );
  });

  it('en fallas del servidor nunca muestra el mensaje técnico (RNF-019)', () => {
    expect(mensajeDeError({ estado: 500, codigo: 'ERROR', mensaje: 'NullPointerException' })).not.toContain('Null');
  });
});

describe('marcarErroresDeCampo', () => {
  it('marca el problema en el control del campo indicado', () => {
    const formulario = new FormGroup({ correo: new FormControl('') });
    marcarErroresDeCampo(formulario, {
      estado: 400,
      codigo: 'VALIDACION',
      mensaje: '',
      detalles: [{ campo: 'correo', problema: 'Formato inválido' }, { campo: 'inexistente', problema: 'x' }],
    });
    expect(formulario.controls.correo.errors).toEqual({ servidor: 'Formato inválido' });
    expect(formulario.controls.correo.touched).toBe(true);
  });
});
