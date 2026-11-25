import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReporteDetalleCiudadano } from '@taan/comun/api';
import { BASE, backend, error, escribir, proveedoresDePrueba, pulsar, texto } from '../../../../testing/utilidades';
import { DetalleReporte } from './detalle-reporte';

const URL_DETALLE = `${BASE}/ciudadano/reportes/F-1`;

const detalle = (cambios: Partial<ReporteDetalleCiudadano> = {}): ReporteDetalleCiudadano => ({
  folio: 'F-1',
  estado: 'recibido',
  estadoTexto: 'Recibido',
  categoria: 'Alumbrado público',
  creadoEn: '2026-11-20T10:00:00Z',
  actualizadoEn: '2026-11-21T10:00:00Z',
  descripcion: 'La lámpara de la esquina no enciende desde hace una semana.',
  ubicacion: { latitud: 18.85, longitud: -97.06 },
  fotos: [],
  organizacion: 'Ayuntamiento de Ixtaczoquitlán',
  area: 'Alumbrado Público',
  historial: [{ fecha: '2026-11-20T10:00:00Z', estado: 'recibido', estadoTexto: 'Recibido' }],
  ...cambios,
});
const resuelto = (permitida = true) =>
  detalle({ estado: 'resuelto', estadoTexto: 'Resuelto', reapertura: { permitida, venceEn: '2026-12-05T10:00:00Z' } });

describe('DetalleReporte', () => {
  let fixture: ComponentFixture<DetalleReporte>;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: proveedoresDePrueba });
    fixture = TestBed.createComponent(DetalleReporte);
    fixture.componentRef.setInput('folio', 'F-1');
    TestBed.tick();
  });

  async function responder(reporte: ReporteDetalleCiudadano) {
    backend().expectOne(URL_DETALLE).flush(reporte);
    await fixture.whenStable();
  }

  it('muestra folio, estado, descripción, quién lo atiende e historial', async () => {
    await responder(detalle());
    expect(texto(fixture)).toContain('Folio F-1');
    expect(texto(fixture)).toContain('La lámpara de la esquina no enciende');
    expect(texto(fixture)).toContain('Ayuntamiento de Ixtaczoquitlán · Alumbrado Público');
    expect(fixture.nativeElement.querySelectorAll('taan-historial li').length).toBe(1);
    expect(texto(fixture)).not.toContain('¿Se resolvió el problema?');
  });

  it('si no existe o no es del ciudadano lo informa (404)', async () => {
    const { cuerpo, opciones } = error(404, 'NO_ENCONTRADO');
    backend().expectOne(URL_DETALLE).flush(cuerpo, opciones);
    await fixture.whenStable();
    expect(texto(fixture)).toContain('No encontramos lo que buscas');
  });

  it('rechazado: muestra el motivo y el contacto del área', async () => {
    await responder(
      detalle({
        estado: 'rechazado',
        estadoTexto: 'Rechazado',
        rechazo: { motivo: 'ya_atendido', texto: 'Se reparó el lunes.', contactoArea: { telefono: '2721234567' } },
      }),
    );
    expect(texto(fixture)).toContain('El problema ya había sido atendido.');
    expect(texto(fixture)).toContain('Se reparó el lunes.');
    expect(fixture.nativeElement.querySelector('a[href="tel:2721234567"]')).not.toBeNull();
  });

  it('canalización externa: muestra los canales oficiales con enlaces seguros', async () => {
    await responder(
      detalle({
        estado: 'canalizacion_externa',
        canalesExternos: [
          { id: 1, organizacionId: 9, organizacion: 'CFE', tipo: 'telefono', valor: '071', fuenteVerificacion: 'cfe.mx', activo: true },
          { id: 2, organizacionId: 9, tipo: 'sitio_web', valor: 'javascript:alert(1)', fuenteVerificacion: 'x', activo: true },
        ],
      }),
    );
    expect(fixture.nativeElement.querySelector('a[href="tel:071"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('a[href^="javascript"]')).toBeNull();
  });

  it('duplicado: muestra el seguimiento del reporte principal', async () => {
    await responder(
      detalle({
        estado: 'duplicado',
        reportePrincipal: { folio: 'F-0', estadoTexto: 'En atención', historial: [] },
      }),
    );
    expect(texto(fixture)).toContain('F-0');
    expect(texto(fixture)).toContain('En atención');
  });

  it('resuelto: el ciudadano confirma la resolución y se actualiza el detalle', async () => {
    await responder(resuelto());
    pulsar(fixture, 'Sí, se resolvió');
    backend().expectOne(`${URL_DETALLE}/confirmacion`).flush(null, { status: 204, statusText: 'No Content' });
    TestBed.tick();
    await responder(resuelto(false));

    expect(texto(fixture)).toContain('Gracias por confirmar');
    expect(texto(fixture)).not.toContain('¿Se resolvió el problema?');
  });

  it('resuelto: el ciudadano lo reabre con un motivo', async () => {
    await responder(resuelto());
    pulsar(fixture, 'No, sigue igual');
    await fixture.whenStable();
    escribir(fixture, '#motivo', 'La lámpara volvió a apagarse.');
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    TestBed.tick();

    const peticion = backend().expectOne(`${URL_DETALLE}/reapertura`);
    expect(peticion.request.body).toEqual({ motivo: 'La lámpara volvió a apagarse.' });
    peticion.flush(detalle({ estado: 'reabierto', estadoTexto: 'Reabierto' }));
    await fixture.whenStable();

    expect(texto(fixture)).toContain('Reabierto');
    expect(texto(fixture)).not.toContain('¿Se resolvió el problema?');
  });

  it('el motivo de reapertura es obligatorio', async () => {
    await responder(resuelto());
    pulsar(fixture, 'No, sigue igual');
    await fixture.whenStable();
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    await fixture.whenStable();
    expect(texto(fixture)).toContain('Este dato es obligatorio.');
    backend().expectNone(`${URL_DETALLE}/reapertura`);
  });

  it('si ya se había confirmado (409 YA_CONFIRMADO) lo explica y recarga el detalle', async () => {
    await responder(resuelto());
    pulsar(fixture, 'No, sigue igual');
    await fixture.whenStable();
    escribir(fixture, '#motivo', 'La lámpara volvió a apagarse.');
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    TestBed.tick();
    const { cuerpo, opciones } = error(409, 'YA_CONFIRMADO');
    backend().expectOne(`${URL_DETALLE}/reapertura`).flush(cuerpo, opciones);
    TestBed.tick();
    await responder(resuelto(false));

    expect(texto(fixture)).toContain('Ya confirmaste que este problema se resolvió');
  });
});
