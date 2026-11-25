import { TestBed } from '@angular/core/testing';

import { ReporteResumenCiudadano } from '@taan/comun/api';
import { BASE, backend, proveedoresDePrueba, pulsar, texto } from '../../../../testing/utilidades';
import { MisReportes } from './mis-reportes';

const reporte = (folio: string): ReporteResumenCiudadano => ({
  folio,
  estado: 'recibido',
  estadoTexto: 'Recibido',
  categoria: 'Alumbrado público',
  creadoEn: '2026-11-20T10:00:00Z',
  actualizadoEn: '2026-11-21T10:00:00Z',
});
const pagina = (contenido: ReporteResumenCiudadano[], numero: number, totalPaginas: number) => ({
  contenido,
  pagina: numero,
  tamano: 20,
  totalElementos: contenido.length,
  totalPaginas,
});

describe('MisReportes', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: proveedoresDePrueba }));

  it('lista los reportes con enlace a su detalle y carga más páginas', async () => {
    const fixture = TestBed.createComponent(MisReportes);
    backend().expectOne(`${BASE}/ciudadano/reportes?pagina=0&tamano=20`).flush(pagina([reporte('F-1')], 0, 2));
    await fixture.whenStable();

    const enlace = fixture.nativeElement.querySelector('a[href="/mis-reportes/F-1"]') as HTMLAnchorElement;
    expect(enlace.textContent).toContain('Recibido');
    expect(enlace.textContent).toContain('Alumbrado público');

    pulsar(fixture, 'Ver más reportes');
    backend().expectOne(`${BASE}/ciudadano/reportes?pagina=1&tamano=20`).flush(pagina([reporte('F-2')], 1, 2));
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelectorAll('li').length).toBe(2);
    expect(texto(fixture)).not.toContain('Ver más reportes');
  });

  it('sin reportes muestra un mensaje', async () => {
    const fixture = TestBed.createComponent(MisReportes);
    backend().expectOne(`${BASE}/ciudadano/reportes?pagina=0&tamano=20`).flush(pagina([], 0, 0));
    await fixture.whenStable();
    expect(texto(fixture)).toContain('Aún no tienes reportes.');
  });

  it('si falla la consulta permite reintentar', async () => {
    const fixture = TestBed.createComponent(MisReportes);
    backend().expectOne(`${BASE}/ciudadano/reportes?pagina=0&tamano=20`).error(new ProgressEvent('error'), { status: 0 });
    await fixture.whenStable();
    expect(texto(fixture)).toContain('Revisa tu conexión');

    pulsar(fixture, 'Intentar de nuevo');
    backend().expectOne(`${BASE}/ciudadano/reportes?pagina=0&tamano=20`).flush(pagina([reporte('F-1')], 0, 1));
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('a[href="/mis-reportes/F-1"]')).not.toBeNull();
  });
});
