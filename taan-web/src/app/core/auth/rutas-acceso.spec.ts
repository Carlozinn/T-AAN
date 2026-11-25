import { destinoSeguro, rutaDeAcceso } from './rutas-acceso';

describe('rutaDeAcceso', () => {
  it('lleva al acceso institucional desde /institucional y /admin, y al ciudadano en lo demás', () => {
    expect(rutaDeAcceso('/institucional/bandeja')).toBe('/institucional/acceso');
    expect(rutaDeAcceso('/admin/zonas')).toBe('/institucional/acceso');
    expect(rutaDeAcceso('/mis-reportes/F-1')).toBe('/acceso');
  });
});

describe('destinoSeguro', () => {
  it('acepta rutas internas', () => {
    expect(destinoSeguro('/mis-reportes/F-1', '/inicio')).toBe('/mis-reportes/F-1');
  });

  it('rechaza destinos externos o vacíos', () => {
    for (const volver of ['https://otro.mx', '//otro.mx', '/\\otro.mx', '', null, undefined]) {
      expect(destinoSeguro(volver, '/inicio')).toBe('/inicio');
    }
  });
});
