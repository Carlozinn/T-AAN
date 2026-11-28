import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Perfil } from '@taan/comun/api';
import { PerfilCiudadano } from '@taan/comun/cuenta/perfil-ciudadano';
import { BASE, backend, error, escribir, proveedoresDePrueba, texto } from '../../../../testing/utilidades';
import { ESPERA_COBERTURA_MS, NuevoReporte } from './nuevo-reporte';

const PERFIL: Perfil = {
  nombre: 'Ana',
  correo: 'ana@correo.mx',
  metodoAcceso: 'correo',
  telefono: '2721234567',
  avisosCorreo: true,
  estado: 'activa',
};
const URL_COBERTURA = `${BASE}/cobertura?latitud=18.85&longitud=-97.06`;
const DESCRIPCION = 'La lámpara de la esquina no enciende desde hace una semana.';

const esperarCobertura = () => new Promise((r) => setTimeout(r, ESPERA_COBERTURA_MS + 50));

function simularGps(permitido: boolean): void {
  Object.defineProperty(navigator, 'geolocation', {
    configurable: true,
    value: {
      getCurrentPosition: (ok: PositionCallback, falla: PositionErrorCallback) =>
        permitido
          ? ok({ coords: { latitude: 18.85, longitude: -97.06 } } as GeolocationPosition)
          : falla({ code: 1 } as GeolocationPositionError),
    },
  });
}

describe('NuevoReporte', () => {
  let fixture: ComponentFixture<NuevoReporte>;

  beforeEach(() => {
    URL.createObjectURL = vi.fn(() => 'blob:vista-previa');
    URL.revokeObjectURL = vi.fn();
    TestBed.configureTestingModule({ providers: [...proveedoresDePrueba, PerfilCiudadano] });
  });

  /** Crea la pantalla con el perfil indicado y, si hay GPS, responde la consulta de cobertura. */
  async function crear(perfil: Perfil = PERFIL, { gps = true, dentro = true } = {}) {
    simularGps(gps);
    fixture = TestBed.createComponent(NuevoReporte);
    TestBed.tick();
    backend().expectOne(`${BASE}/ciudadano/perfil`).flush(perfil);
    if (gps) {
      await esperarCobertura();
      backend().expectOne(URL_COBERTURA).flush({ dentroDeCobertura: dentro, zona: dentro ? 'Cabecera' : null });
    }
    await fixture.whenStable();
  }

  const enviar = () => {
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    TestBed.tick();
  };

  async function datosEnviados(): Promise<{ datos: unknown; fotos: File[] }> {
    const cuerpo = backend().expectOne(`${BASE}/ciudadano/reportes`).request.body as FormData;
    return { datos: JSON.parse(await (cuerpo.get('datos') as Blob).text()), fotos: cuerpo.getAll('fotos') as File[] };
  }

  function elegirFotos(...archivos: File[]): void {
    const entrada = fixture.nativeElement.querySelector('#fotos') as HTMLInputElement;
    Object.defineProperty(entrada, 'files', { configurable: true, value: archivos });
    entrada.dispatchEvent(new Event('change'));
  }

  it('con GPS ubica el problema, confirma la cobertura y envía todo en un paso; muestra el folio', async () => {
    await crear();
    expect(texto(fixture)).toContain('Ubicación dentro de la zona de atención: Cabecera');
    expect(fixture.nativeElement.querySelector('#telefono')).toBeNull();

    escribir(fixture, '#descripcion', DESCRIPCION);
    elegirFotos(new File(['x'], 'foto.jpg', { type: 'image/jpeg' }));
    enviar();

    const peticion = backend().expectOne(`${BASE}/ciudadano/reportes`);
    const cuerpo = peticion.request.body as FormData;
    expect(JSON.parse(await (cuerpo.get('datos') as Blob).text())).toEqual({
      descripcion: DESCRIPCION,
      ubicacion: { latitud: 18.85, longitud: -97.06 },
      origen: 'web',
      campoAuxiliar: '',
    });
    expect((cuerpo.getAll('fotos') as File[]).map((f) => f.name)).toEqual(['foto.jpg']);
    peticion.flush({ folio: 'F-2026-000045', estado: 'pendiente_canalizacion', estadoTexto: 'En revisión', creadoEn: '' });
    await fixture.whenStable();

    expect(texto(fixture)).toContain('Tu folio es F-2026-000045');
    expect(fixture.nativeElement.querySelector('a[href="/mis-reportes/F-2026-000045"]')).not.toBeNull();
  });

  it('fuera de cobertura avisa antes de enviar (con el 911) y no deja enviar', async () => {
    await crear(PERFIL, { dentro: false });
    expect(texto(fixture)).toContain('T’AAN todavía no opera en esta ubicación. Si es una emergencia, llama al 911.');
    expect(fixture.nativeElement.querySelector('button[type=submit]').disabled).toBe(true);
  });

  it('sin permiso de ubicación pide marcarla en el mapa', async () => {
    await crear(PERFIL, { gps: false });
    expect(texto(fixture)).toContain('No pudimos obtener tu ubicación.');
    escribir(fixture, '#descripcion', DESCRIPCION);
    enviar();
    await fixture.whenStable();
    expect(texto(fixture)).toContain('Marca en el mapa dónde está el problema.');
    backend().expectNone(`${BASE}/ciudadano/reportes`);
  });

  it('sin teléfono en el perfil lo pide en el mismo formulario, lo guarda y luego envía el reporte', async () => {
    await crear({ ...PERFIL, telefono: null });
    escribir(fixture, '#descripcion', DESCRIPCION);
    enviar();
    await fixture.whenStable();
    expect(texto(fixture)).toContain('Este dato es obligatorio.');
    backend().expectNone(`${BASE}/ciudadano/perfil`);

    escribir(fixture, '#telefono', '2729876543');
    enviar();
    const perfil = backend().expectOne((r) => r.method === 'PATCH' && r.url === `${BASE}/ciudadano/perfil`);
    expect(perfil.request.body).toEqual({ telefono: '2729876543' });
    perfil.flush({ ...PERFIL, telefono: '2729876543' });

    const { datos } = await datosEnviados();
    expect(datos).toMatchObject({ descripcion: DESCRIPCION });
  });

  it('límite excedido (429): mensaje preventivo con el 911 y conserva lo escrito', async () => {
    await crear();
    escribir(fixture, '#descripcion', DESCRIPCION);
    enviar();
    const { cuerpo, opciones } = error(429, 'LIMITE_EXCEDIDO');
    backend().expectOne(`${BASE}/ciudadano/reportes`).flush(cuerpo, opciones);
    await fixture.whenStable();

    expect(texto(fixture)).toContain('Lo que escribiste se conservó.');
    expect(texto(fixture)).toContain('911');
    expect(fixture.nativeElement.querySelector('#descripcion').value).toBe(DESCRIPCION);
  });

  it('cuenta suspendida (403): muestra el motivo y la vigencia que envía el backend', async () => {
    await crear();
    escribir(fixture, '#descripcion', DESCRIPCION);
    enviar();
    const { cuerpo, opciones } = error(403, 'CUENTA_SUSPENDIDA', 'Tu cuenta está suspendida hasta el 15/12/2026. Motivo: abuso.');
    backend().expectOne(`${BASE}/ciudadano/reportes`).flush(cuerpo, opciones);
    await fixture.whenStable();
    expect(texto(fixture)).toContain('Tu cuenta está suspendida hasta el 15/12/2026. Motivo: abuso.');
  });

  it('correo sin confirmar (403): lo explica y ofrece reenviar el enlace', async () => {
    await crear();
    escribir(fixture, '#descripcion', DESCRIPCION);
    enviar();
    const { cuerpo, opciones } = error(403, 'CORREO_NO_VERIFICADO');
    backend().expectOne(`${BASE}/ciudadano/reportes`).flush(cuerpo, opciones);
    await fixture.whenStable();
    expect(texto(fixture)).toContain('Confirma tu correo');
    expect(texto(fixture)).toContain('Enviar un nuevo enlace');
  });

  it('con la cuenta suspendida no muestra el formulario', async () => {
    await crear({ ...PERFIL, estado: 'suspendida', suspension: { motivo: 'x', hasta: null } }, { gps: false });
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
    expect(texto(fixture)).toContain('no puedes enviar reportes');
  });

  it('fotos: acepta hasta 3 de los tipos permitidos, avisa de las rechazadas y permite quitarlas', async () => {
    await crear();
    elegirFotos(
      new File(['x'], 'a.jpg', { type: 'image/jpeg' }),
      new File(['x'], 'b.png', { type: 'image/png' }),
      new File(['x'], 'c.gif', { type: 'image/gif' }),
    );
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelectorAll('.fotos img').length).toBe(2);
    expect(texto(fixture)).toContain('usa JPG, PNG o WebP de hasta 5 MB');

    elegirFotos(new File(['x'], 'd.webp', { type: 'image/webp' }), new File(['x'], 'e.jpg', { type: 'image/jpeg' }));
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelectorAll('.fotos img').length).toBe(3);
    expect(texto(fixture)).toContain('Puedes adjuntar hasta 3 fotos.');

    (fixture.nativeElement.querySelector('.fotos button') as HTMLButtonElement).click();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelectorAll('.fotos img').length).toBe(2);
    expect(URL.revokeObjectURL).toHaveBeenCalled();
  });

  it('el campo trampa queda oculto para personas y lectores de pantalla', async () => {
    await crear();
    expect(fixture.nativeElement.querySelector('#campo-auxiliar').closest('[aria-hidden=true].trampa')).not.toBeNull();
  });
});
