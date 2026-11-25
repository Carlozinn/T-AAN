import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { Perfil } from '@taan/comun/api';
import { SesionService } from '@taan/comun/sesion/sesion.service';
import { BASE, backend, proveedoresDePrueba, pulsar, texto } from '../../../../testing/utilidades';
import { MarcoCiudadano } from './marco-ciudadano';

const perfil = (cambios: Partial<Perfil> = {}): Perfil => ({
  nombre: 'Ana',
  correo: 'ana@correo.mx',
  metodoAcceso: 'correo',
  avisosCorreo: true,
  estado: 'activa',
  ...cambios,
});

describe('MarcoCiudadano', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: proveedoresDePrueba });
    TestBed.inject(SesionService).iniciar({ accessToken: 'a', refreshToken: 'r', expiraEn: 1, rol: 'ciudadano' });
  });

  async function crear(respuesta: Perfil) {
    const fixture = TestBed.createComponent(MarcoCiudadano);
    TestBed.tick();
    backend().expectOne(`${BASE}/ciudadano/perfil`).flush(respuesta);
    await fixture.whenStable();
    return fixture;
  }

  it('con la cuenta activa no muestra avisos', async () => {
    const fixture = await crear(perfil());
    expect(texto(fixture)).not.toContain('suspendida');
  });

  it('con la cuenta suspendida muestra motivo y vigencia (CU-04 FA-04)', async () => {
    const fixture = await crear(
      perfil({ estado: 'suspendida', suspension: { motivo: 'Reportes falsos', hasta: '2026-12-01T00:00:00Z' } }),
    );
    expect(texto(fixture)).toContain('Tu cuenta está suspendida hasta el');
    expect(texto(fixture)).toContain('Motivo: Reportes falsos.');
    expect(texto(fixture)).toContain('Puedes consultar tus reportes');
  });

  it('suspensión indefinida', async () => {
    const fixture = await crear(perfil({ estado: 'suspendida', suspension: { motivo: 'Abuso', hasta: null } }));
    expect(texto(fixture)).toContain('por tiempo indefinido');
  });

  it('salir cierra la sesión y lleva al acceso', async () => {
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = await crear(perfil());
    pulsar(fixture, 'Salir');
    backend().expectOne(`${BASE}/auth/cierre`).flush(null, { status: 204, statusText: 'No Content' });

    expect(TestBed.inject(SesionService).autenticado()).toBe(false);
    expect(navegar).toHaveBeenCalledWith(['/acceso']);
  });
});
