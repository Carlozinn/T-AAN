import { ComponentFixture, TestBed } from '@angular/core/testing';
import * as L from 'leaflet';

import { MapaUbicacion } from './mapa-ubicacion';

describe('MapaUbicacion', () => {
  let fixture: ComponentFixture<MapaUbicacion>;
  let mapa: L.Map;

  beforeEach(async () => {
    fixture = TestBed.createComponent(MapaUbicacion);
    await fixture.whenStable();
    mapa = fixture.componentInstance['mapa']!;
  });

  const marcador = () => fixture.nativeElement.querySelector('.taan-marcador') as HTMLElement | null;

  it('arranca centrado en el piloto y sin marcador', () => {
    expect(mapa.getCenter().lat).toBeCloseTo(18.85);
    expect(mapa.getCenter().lng).toBeCloseTo(-97.06);
    expect(marcador()).toBeNull();
  });

  it('al tocar el mapa coloca el marcador y emite la ubicación', () => {
    mapa.fire('click', { latlng: L.latLng(18.86, -97.05) });
    expect(fixture.componentInstance.ubicacion()).toEqual({ latitud: 18.86, longitud: -97.05 });
    TestBed.tick();
    expect(marcador()).not.toBeNull();
  });

  it('una ubicación fijada desde fuera (p. ej. GPS) mueve el marcador y centra la vista', () => {
    fixture.componentInstance.ubicacion.set({ latitud: 18.87, longitud: -97.04 });
    TestBed.tick();
    expect(marcador()).not.toBeNull();
    expect(mapa.getCenter().lat).toBeCloseTo(18.87);
    expect(mapa.getZoom()).toBe(17);
  });

  it('marcarCentro usa el centro visible (alternativa sin arrastrar)', () => {
    fixture.componentInstance.marcarCentro();
    expect(fixture.componentInstance.ubicacion()?.latitud).toBeCloseTo(18.85);
  });
});
