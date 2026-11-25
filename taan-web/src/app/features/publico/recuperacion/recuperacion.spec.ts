import { TestBed } from '@angular/core/testing';

import { BASE, backend, error, escribir, proveedoresDePrueba, texto } from '../../../../testing/utilidades';
import { Recuperacion } from './recuperacion';

describe('Recuperacion', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: proveedoresDePrueba }));

  async function crear(token?: string) {
    const fixture = TestBed.createComponent(Recuperacion);
    if (token) {
      fixture.componentRef.setInput('token', token);
    }
    await fixture.whenStable();
    return fixture;
  }

  const enviar = (fixture: Awaited<ReturnType<typeof crear>>) => {
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    TestBed.tick();
  };

  it('sin token solicita el enlace y responde igual exista o no la cuenta', async () => {
    const fixture = await crear();
    escribir(fixture, '#correo', 'ana@correo.mx');
    enviar(fixture);
    const peticion = backend().expectOne(`${BASE}/auth/ciudadanos/recuperacion`);
    expect(peticion.request.body).toEqual({ correo: 'ana@correo.mx' });
    peticion.flush(null, { status: 202, statusText: 'Accepted' });
    await fixture.whenStable();
    expect(texto(fixture)).toContain('Si ese correo tiene una cuenta');
  });

  it('con el token del correo guarda la nueva contraseña', async () => {
    const fixture = await crear('token-del-correo');
    escribir(fixture, '#nueva-password', 'nueva-secreta');
    enviar(fixture);
    const peticion = backend().expectOne(`${BASE}/auth/ciudadanos/recuperacion/confirmacion`);
    expect(peticion.request.body).toEqual({ token: 'token-del-correo', nuevaPassword: 'nueva-secreta' });
    peticion.flush(null, { status: 204, statusText: 'No Content' });
    await fixture.whenStable();
    expect(texto(fixture)).toContain('Tu contraseña se actualizó.');
  });

  it('con el enlace vencido lo informa y ofrece pedir otro', async () => {
    const fixture = await crear('vencido');
    escribir(fixture, '#nueva-password', 'nueva-secreta');
    enviar(fixture);
    const { cuerpo, opciones } = error(422, 'ENLACE_VENCIDO');
    backend().expectOne(`${BASE}/auth/ciudadanos/recuperacion/confirmacion`).flush(cuerpo, opciones);
    await fixture.whenStable();
    expect(texto(fixture)).toContain('El enlace ya no es válido.');
    expect(texto(fixture)).toContain('Pedir un enlace nuevo');
  });
});
