import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';

import { provideApi } from '@taan/comun/api';
import { SesionService } from '@taan/comun/sesion/sesion.service';
import { rolGuard } from './rol.guard';

function evaluar(url: string, ...roles: Parameters<typeof rolGuard>) {
  return TestBed.runInInjectionContext(() =>
    rolGuard(...roles)({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot),
  );
}

describe('rolGuard', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), provideApi('http://api')],
    });
  });

  it('deja pasar con un rol permitido', () => {
    TestBed.inject(SesionService).iniciar({ accessToken: 'a', refreshToken: 'r', expiraEn: 1, rol: 'coordinacion' });
    expect(evaluar('/institucional/bandeja', 'personal_area', 'coordinacion')).toBe(true);
  });

  it('sin sesión lleva al acceso ciudadano y recuerda a dónde volver', () => {
    const arbol = evaluar('/mis-reportes/F-1', 'ciudadano') as UrlTree;
    expect(TestBed.inject(Router).serializeUrl(arbol)).toBe('/acceso?volver=%2Fmis-reportes%2FF-1');
  });

  it('en secciones institucionales lleva al acceso institucional', () => {
    TestBed.inject(SesionService).iniciar({ accessToken: 'a', refreshToken: 'r', expiraEn: 1, rol: 'ciudadano' });
    const arbol = evaluar('/admin/zonas', 'administrador') as UrlTree;
    expect(TestBed.inject(Router).serializeUrl(arbol)).toBe('/institucional/acceso?volver=%2Fadmin%2Fzonas');
  });
});
