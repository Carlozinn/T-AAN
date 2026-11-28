import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ApplicationRef } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { Perfil, provideApi } from '../api';
import { PerfilCiudadano, describirSuspension } from './perfil-ciudadano';

const BASE = 'http://api';
const PERFIL: Perfil = { nombre: 'Ana', correo: 'ana@correo.mx', metodoAcceso: 'correo', avisosCorreo: true, estado: 'activa' };

describe('PerfilCiudadano', () => {
  beforeEach(() =>
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideApi(BASE), PerfilCiudadano],
    }),
  );

  it('consulta el perfil una vez y acepta la versión actualizada', async () => {
    const perfil = TestBed.inject(PerfilCiudadano);
    TestBed.tick();
    TestBed.inject(HttpTestingController).expectOne(`${BASE}/ciudadano/perfil`).flush(PERFIL);
    await TestBed.inject(ApplicationRef).whenStable();
    expect(perfil.perfil()?.nombre).toBe('Ana');

    perfil.actualizar({ ...PERFIL, telefono: '2721234567' });
    expect(perfil.perfil()?.telefono).toBe('2721234567');
    expect(perfil.suspendida()).toBe(false);
  });
});

describe('describirSuspension', () => {
  it('con vigencia y motivo', () => {
    const texto = describirSuspension(
      { ...PERFIL, estado: 'suspendida', suspension: { motivo: 'Reportes falsos', hasta: '2026-12-15T12:00:00Z' } },
      'en-US',
    );
    expect(texto).toBe('Tu cuenta está suspendida hasta el December 15, 2026. Motivo: Reportes falsos.');
  });

  it('indefinida', () => {
    expect(describirSuspension({ ...PERFIL, estado: 'suspendida', suspension: { hasta: null } }, 'en-US')).toBe(
      'Tu cuenta está suspendida por tiempo indefinido.',
    );
  });

  it('activa', () => {
    expect(describirSuspension(PERFIL, 'en-US')).toBeNull();
  });
});
