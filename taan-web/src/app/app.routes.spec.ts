import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Router, provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { provideApi } from '@taan/comun/api';
import { SesionService } from '@taan/comun/sesion/sesion.service';
import { routes } from './app.routes';
import { DetalleReporte } from './features/ciudadano/detalle-reporte/detalle-reporte';

/** El backend envía por correo enlaces a /mis-reportes/{folio}: deben abrir el detalle, con o sin sesión previa. */
describe('Rutas de la app', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes, withComponentInputBinding()),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideApi('http://api'),
      ],
    });
  });

  it('la raíz sin sesión lleva al inicio de sesión', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/');
    expect(TestBed.inject(Router).url).toBe('/acceso?volver=%2Freportar');
  });

  it('la raíz con sesión ciudadana lleva directo a reportar (mínima interacción)', async () => {
    TestBed.inject(SesionService).iniciar({ accessToken: 'a', refreshToken: 'r', expiraEn: 1, rol: 'ciudadano' });
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/');
    expect(TestBed.inject(Router).url).toBe('/reportar');
  });

  it('/mis-reportes/{folio} sin sesión pide iniciarla y conserva el destino', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/mis-reportes/F-2026-000123');
    expect(TestBed.inject(Router).url).toBe('/acceso?volver=%2Fmis-reportes%2FF-2026-000123');
  });

  it('/mis-reportes/{folio} con sesión ciudadana abre el detalle de ese folio', async () => {
    TestBed.inject(SesionService).iniciar({ accessToken: 'a', refreshToken: 'r', expiraEn: 1, rol: 'ciudadano' });
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/mis-reportes/F-2026-000123');
    const detalle = harness.fixture.debugElement.query(By.directive(DetalleReporte));
    expect((detalle.componentInstance as DetalleReporte).folio()).toBe('F-2026-000123');
  });

  it('los enlaces de verificación y recuperación son públicos', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/verificacion?token=abc');
    expect(TestBed.inject(Router).url).toBe('/verificacion?token=abc');
    await harness.navigateByUrl('/recuperacion?token=abc');
    expect(TestBed.inject(Router).url).toBe('/recuperacion?token=abc');
  });
});
