import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { Sesion, provideApi } from '../api';
import { AL_EXPIRAR_SESION, authInterceptor } from './auth.interceptor';
import { SesionService } from './sesion.service';

const BASE = 'http://api';
const SESION: Sesion = { accessToken: 'acceso-1', refreshToken: 'refresco-1', expiraEn: 900, rol: 'ciudadano' };
const NO_AUTORIZADO = { status: 401, statusText: 'Unauthorized' };

describe('authInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let sesion: SesionService;
  const alExpirar = vi.fn();

  beforeEach(() => {
    localStorage.clear();
    alExpirar.mockReset();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideApi(BASE),
        { provide: AL_EXPIRAR_SESION, useValue: alExpirar },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    sesion = TestBed.inject(SesionService);
  });

  afterEach(() => backend.verify());

  it('agrega el token solo a las peticiones al backend de T’AAN', () => {
    sesion.iniciar(SESION);
    http.get(`${BASE}/ciudadano/perfil`).subscribe();
    http.get('https://otro.mx/recurso').subscribe();
    expect(backend.expectOne(`${BASE}/ciudadano/perfil`).request.headers.get('Authorization')).toBe('Bearer acceso-1');
    expect(backend.expectOne('https://otro.mx/recurso').request.headers.has('Authorization')).toBe(false);
  });

  it('ante 401 renueva una sola vez y reintenta con el token nuevo', () => {
    sesion.iniciar(SESION);
    const respuestas: unknown[] = [];
    http.get(`${BASE}/a`).subscribe((r) => respuestas.push(r));
    http.get(`${BASE}/b`).subscribe((r) => respuestas.push(r));
    backend.expectOne(`${BASE}/a`).flush(null, NO_AUTORIZADO);
    backend.expectOne(`${BASE}/b`).flush(null, NO_AUTORIZADO);

    backend.expectOne(`${BASE}/auth/refresco`).flush({ ...SESION, accessToken: 'acceso-2' });

    const reintentoA = backend.expectOne(`${BASE}/a`);
    const reintentoB = backend.expectOne(`${BASE}/b`);
    expect(reintentoA.request.headers.get('Authorization')).toBe('Bearer acceso-2');
    expect(reintentoB.request.headers.get('Authorization')).toBe('Bearer acceso-2');
    reintentoA.flush('A');
    reintentoB.flush('B');
    expect(respuestas).toEqual(['A', 'B']);
  });

  it('si el refreshToken se rechaza, borra la sesión, avisa y entrega el 401 original', () => {
    sesion.iniciar(SESION);
    let error: HttpErrorResponse | undefined;
    http.get(`${BASE}/a`).subscribe({ error: (e) => (error = e) });
    backend.expectOne(`${BASE}/a`).flush(null, NO_AUTORIZADO);
    backend
      .expectOne(`${BASE}/auth/refresco`)
      .flush({ codigo: 'REFRESCO_INVALIDO', mensaje: '' }, { status: 422, statusText: 'Unprocessable' });

    expect(sesion.autenticado()).toBe(false);
    expect(alExpirar).toHaveBeenCalledOnce();
    expect(error?.status).toBe(401);
  });

  it('sin sesión no intenta renovar', () => {
    let error: HttpErrorResponse | undefined;
    http.get(`${BASE}/a`).subscribe({ error: (e) => (error = e) });
    backend.expectOne(`${BASE}/a`).flush(null, NO_AUTORIZADO);
    expect(error?.status).toBe(401);
    expect(alExpirar).not.toHaveBeenCalled();
  });
});
