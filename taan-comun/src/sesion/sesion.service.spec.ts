import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { Sesion, provideApi } from '../api';
import { SesionService } from './sesion.service';

const BASE = 'http://api';
const SESION: Sesion = { accessToken: 'acceso-1', refreshToken: 'refresco-1', expiraEn: 900, rol: 'ciudadano' };

describe('SesionService', () => {
  let servicio: SesionService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideApi(BASE)] });
    servicio = TestBed.inject(SesionService);
    http = TestBed.inject(HttpTestingController);
  });

  it('iniciar guarda la sesión y expone el rol', () => {
    servicio.iniciar(SESION);
    expect(servicio.rol()).toBe('ciudadano');
    expect(JSON.parse(localStorage.getItem('taan.sesion')!)).toEqual(SESION);
  });

  it('salir avisa al backend y borra la sesión aunque esa llamada falle', () => {
    servicio.iniciar(SESION);
    let termino = false;
    servicio.salir().subscribe(() => (termino = true));
    http.expectOne(`${BASE}/auth/cierre`).flush(null, { status: 500, statusText: 'Error' });
    expect(termino).toBe(true);
    expect(servicio.autenticado()).toBe(false);
    expect(localStorage.getItem('taan.sesion')).toBeNull();
  });

  it('renovar guarda la sesión nueva', () => {
    servicio.iniciar(SESION);
    servicio.renovar().subscribe();
    const peticion = http.expectOne(`${BASE}/auth/refresco`);
    expect(peticion.request.body).toEqual({ refreshToken: 'refresco-1' });
    peticion.flush({ ...SESION, accessToken: 'acceso-2' });
    expect(servicio.sesion()?.accessToken).toBe('acceso-2');
  });

  it('si la renovación falla por la red, conserva la sesión para reintentar', () => {
    servicio.iniciar(SESION);
    servicio.renovar().subscribe({ error: () => undefined });
    http.expectOne(`${BASE}/auth/refresco`).error(new ProgressEvent('error'));
    expect(servicio.autenticado()).toBe(true);
  });
});
