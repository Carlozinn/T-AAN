import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { Perfil as PerfilApi } from '@taan/comun/api';
import { PerfilCiudadano } from '@taan/comun/cuenta/perfil-ciudadano';
import { SesionService } from '@taan/comun/sesion/sesion.service';
import { BASE, backend, error, escribir, proveedoresDePrueba, pulsar, texto } from '../../../../testing/utilidades';
import { Perfil } from './perfil';

const PERFIL: PerfilApi = {
  nombre: 'Ana',
  correo: 'ana@correo.mx',
  metodoAcceso: 'correo',
  telefono: '2721234567',
  avisosCorreo: false,
  estado: 'activa',
};

describe('Perfil', () => {
  let fixture: ComponentFixture<Perfil>;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [...proveedoresDePrueba, PerfilCiudadano] });
    TestBed.inject(SesionService).iniciar({ accessToken: 'a', refreshToken: 'r', expiraEn: 1, rol: 'ciudadano' });
  });

  async function crear(perfil: PerfilApi = PERFIL) {
    fixture = TestBed.createComponent(Perfil);
    TestBed.tick();
    backend().expectOne(`${BASE}/ciudadano/perfil`).flush(perfil);
    await fixture.whenStable();
  }

  const enviar = (selector: string) => {
    fixture.nativeElement.querySelector(selector).closest('form').dispatchEvent(new Event('submit'));
    TestBed.tick();
  };

  it('muestra los datos y guarda nombre, teléfono y avisos', async () => {
    await crear();
    expect(texto(fixture)).toContain('ana@correo.mx');
    expect(fixture.nativeElement.querySelector('#nombre').value).toBe('Ana');
    expect(fixture.nativeElement.querySelector('#telefono').value).toBe('2721234567');
    expect(texto(fixture)).toContain('Activa.');

    escribir(fixture, '#nombre', 'Ana López');
    fixture.nativeElement.querySelector('#avisos').click();
    enviar('#nombre');
    const peticion = backend().expectOne((r) => r.method === 'PATCH');
    expect(peticion.request.body).toEqual({ nombre: 'Ana López', telefono: '2721234567', avisosCorreo: true });
    peticion.flush({ ...PERFIL, nombre: 'Ana López', avisosCorreo: true });
    await fixture.whenStable();

    expect(texto(fixture)).toContain('Guardamos tus cambios.');
    expect(TestBed.inject(PerfilCiudadano).perfil()?.nombre).toBe('Ana López');
  });

  it('valida el teléfono de 10 dígitos antes de enviar', async () => {
    await crear();
    escribir(fixture, '#telefono', '272-123');
    enviar('#nombre');
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('#telefono').getAttribute('aria-invalid')).toBe('true');
    backend().expectNone((r) => r.method === 'PATCH');
  });

  it('muestra la suspensión con motivo y vigencia', async () => {
    await crear({ ...PERFIL, estado: 'suspendida', suspension: { motivo: 'Reportes falsos', hasta: null } });
    expect(texto(fixture)).toContain('Tu cuenta está suspendida por tiempo indefinido. Motivo: Reportes falsos.');
  });

  it('cambia la contraseña y muestra si la actual no coincide', async () => {
    await crear();
    escribir(fixture, '#password-actual', 'vieja-123');
    escribir(fixture, '#nueva-password', 'nueva-secreta');
    enviar('#password-actual');
    const peticion = backend().expectOne(`${BASE}/ciudadano/password`);
    expect(peticion.request.body).toEqual({ passwordActual: 'vieja-123', nuevaPassword: 'nueva-secreta' });
    const { cuerpo, opciones } = error(400, 'PASSWORD_ACTUAL_INCORRECTA', 'La contraseña actual no coincide.');
    peticion.flush(cuerpo, opciones);
    await fixture.whenStable();
    expect(texto(fixture)).toContain('La contraseña actual no coincide.');

    enviar('#password-actual');
    backend().expectOne(`${BASE}/ciudadano/password`).flush(null, { status: 204, statusText: 'No Content' });
    await fixture.whenStable();
    expect(texto(fixture)).toContain('Tu contraseña se actualizó.');
  });

  it('las cuentas de Google no tienen cambio de contraseña', async () => {
    await crear({ ...PERFIL, metodoAcceso: 'google' });
    expect(fixture.nativeElement.querySelector('#password-actual')).toBeNull();
    expect(texto(fixture)).toContain('Entras con tu cuenta de Google');
  });

  it('eliminar la cuenta exige escribir ELIMINAR; después cierra la sesión y lo informa', async () => {
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    await crear();
    pulsar(fixture, 'Eliminar mi cuenta');
    await fixture.whenStable();

    escribir(fixture, '#confirmacion', 'eliminar');
    enviar('#confirmacion');
    await fixture.whenStable();
    expect(texto(fixture)).toContain('Escribe ELIMINAR, en mayúsculas');
    backend().expectNone(`${BASE}/ciudadano/cuenta/eliminacion`);

    escribir(fixture, '#confirmacion', 'ELIMINAR');
    enviar('#confirmacion');
    const peticion = backend().expectOne(`${BASE}/ciudadano/cuenta/eliminacion`);
    expect(peticion.request.body).toEqual({ confirmacion: 'ELIMINAR' });
    peticion.flush(null, { status: 204, statusText: 'No Content' });

    expect(TestBed.inject(SesionService).autenticado()).toBe(false);
    expect(navegar).toHaveBeenCalledWith(['/acceso'], { queryParams: { cuentaEliminada: 1 } });
  });
});
