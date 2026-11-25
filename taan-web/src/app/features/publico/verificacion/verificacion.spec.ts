import { TestBed } from '@angular/core/testing';

import { BASE, backend, error, escribir, proveedoresDePrueba, texto } from '../../../../testing/utilidades';
import { Verificacion } from './verificacion';

describe('Verificacion', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: proveedoresDePrueba }));

  function crear(token?: string) {
    const fixture = TestBed.createComponent(Verificacion);
    if (token) {
      fixture.componentRef.setInput('token', token);
    }
    TestBed.tick();
    return fixture;
  }

  it('confirma el token del enlace', async () => {
    const fixture = crear('token-del-correo');
    const peticion = backend().expectOne(`${BASE}/auth/ciudadanos/verificacion`);
    expect(peticion.request.body).toEqual({ token: 'token-del-correo' });
    peticion.flush(null, { status: 204, statusText: 'No Content' });
    await fixture.whenStable();
    expect(texto(fixture)).toContain('Tu correo quedó confirmado.');
  });

  it('con el enlace vencido permite pedir uno nuevo', async () => {
    const fixture = crear('vencido');
    const { cuerpo, opciones } = error(422, 'ENLACE_VENCIDO');
    backend().expectOne(`${BASE}/auth/ciudadanos/verificacion`).flush(cuerpo, opciones);
    await fixture.whenStable();
    expect(texto(fixture)).toContain('El enlace ya no es válido.');

    escribir(fixture, '#correo-reenvio', 'ana@correo.mx');
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    TestBed.tick();
    const reenvio = backend().expectOne(`${BASE}/auth/ciudadanos/verificacion/reenvio`);
    expect(reenvio.request.body).toEqual({ correo: 'ana@correo.mx' });
    reenvio.flush(null, { status: 202, statusText: 'Accepted' });
    await fixture.whenStable();
    expect(texto(fixture)).toContain('Te enviamos un nuevo enlace.');
  });

  it('sin token no llama al backend', async () => {
    const fixture = crear();
    await fixture.whenStable();
    backend().expectNone(`${BASE}/auth/ciudadanos/verificacion`);
    expect(texto(fixture)).toContain('pide uno nuevo');
  });
});
