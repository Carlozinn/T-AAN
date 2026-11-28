import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { Sesion } from '@taan/comun/api';
import { SesionService } from '@taan/comun/sesion/sesion.service';
import { BASE, backend, error, escribir, proveedoresDePrueba, texto } from '../../../../testing/utilidades';
import { Acceso } from './acceso';

const SESION: Sesion = { accessToken: 'a', refreshToken: 'r', expiraEn: 900, rol: 'ciudadano' };

describe('Acceso', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: proveedoresDePrueba });
  });

  async function crear(volver?: string) {
    const fixture = TestBed.createComponent(Acceso);
    if (volver) {
      fixture.componentRef.setInput('volver', volver);
    }
    await fixture.whenStable();
    return fixture;
  }

  async function entrar(fixture: Awaited<ReturnType<typeof crear>>) {
    escribir(fixture, '#correo', 'ana@correo.mx');
    escribir(fixture, '#password', 'secreta123');
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    TestBed.tick();
  }

  it('sin datos marca los campos y no llama al backend', async () => {
    const fixture = await crear();
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    await fixture.whenStable();
    expect(texto(fixture)).toContain('Este dato es obligatorio.');
    backend().expectNone(`${BASE}/auth/ciudadanos/sesion`);
  });

  it('inicia sesión y vuelve a la página pedida (p. ej. el enlace del correo)', async () => {
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const fixture = await crear('/mis-reportes/F-1');
    await entrar(fixture);

    const peticion = backend().expectOne(`${BASE}/auth/ciudadanos/sesion`);
    expect(peticion.request.body).toEqual({ correo: 'ana@correo.mx', password: 'secreta123' });
    peticion.flush(SESION);

    expect(TestBed.inject(SesionService).sesion()).toEqual(SESION);
    expect(navegar).toHaveBeenCalledWith('/mis-reportes/F-1');
  });

  it('sin destino seguro va a reportar', async () => {
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const fixture = await crear('https://otro.mx');
    await entrar(fixture);
    backend().expectOne(`${BASE}/auth/ciudadanos/sesion`).flush(SESION);
    expect(navegar).toHaveBeenCalledWith('/reportar');
  });

  it('después de eliminar la cuenta lo confirma', async () => {
    const fixture = TestBed.createComponent(Acceso);
    fixture.componentRef.setInput('cuentaEliminada', '1');
    await fixture.whenStable();
    expect(texto(fixture)).toContain('Tu cuenta se eliminó.');
  });

  it('muestra el mensaje del backend si las credenciales no coinciden', async () => {
    const fixture = await crear();
    await entrar(fixture);
    const { cuerpo, opciones } = error(400, 'CREDENCIALES_INVALIDAS', 'Revisa tu correo y contraseña.');
    backend().expectOne(`${BASE}/auth/ciudadanos/sesion`).flush(cuerpo, opciones);
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('[role=alert]').textContent).toContain('Revisa tu correo y contraseña.');
    expect(TestBed.inject(SesionService).autenticado()).toBe(false);
  });
});
