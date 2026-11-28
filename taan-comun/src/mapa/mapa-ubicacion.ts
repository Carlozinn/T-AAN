import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  InjectionToken,
  ViewEncapsulation,
  afterNextRender,
  effect,
  inject,
  model,
  viewChild,
} from '@angular/core';
import * as L from 'leaflet';

import { Ubicacion } from '../api';

export interface ConfigMapa {
  /** URL de teselas; para producción con más tráfico se cambia por un proveedor propio. */
  teselas: string;
  atribucion: string;
  centro: Ubicacion;
  zoom: number;
}

/** Leaflet con teselas de OpenStreetMap: sin clave ni costo. El centro es la cabecera del piloto. */
export const CONFIG_MAPA = new InjectionToken<ConfigMapa>('CONFIG_MAPA', {
  providedIn: 'root',
  factory: () => ({
    teselas: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
    atribucion: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    centro: { latitud: 18.85, longitud: -97.06 },
    zoom: 14,
  }),
});

const ZOOM_UBICACION = 17;

/**
 * Mapa para marcar dónde está el problema (RF-005): se toca o se hace clic para colocar el
 * marcador, que puede arrastrarse. Sin teclado ni mouse preciso, marcarCentro() usa el centro visible.
 */
@Component({
  selector: 'taan-mapa-ubicacion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  // Leaflet crea su propio DOM: sus estilos y los del marcador deben ser globales.
  encapsulation: ViewEncapsulation.None,
  styleUrls: ['../../../node_modules/leaflet/dist/leaflet.css', './mapa-ubicacion.css'],
  template: `<div #contenedor class="taan-mapa"></div>`,
})
export class MapaUbicacion {
  /** Ubicación marcada; cambia al tocar el mapa o arrastrar el marcador, y se puede fijar desde fuera. */
  readonly ubicacion = model<Ubicacion | null>(null);

  private readonly contenedor = viewChild.required<ElementRef<HTMLDivElement>>('contenedor');
  private readonly config = inject(CONFIG_MAPA);
  private mapa: L.Map | undefined;
  private marcador: L.Marker | undefined;
  /** Si el cambio vino del propio mapa, no se vuelve a centrar la vista. */
  private cambioDesdeMapa = false;

  constructor() {
    afterNextRender(() => {
      const { centro, zoom, teselas, atribucion } = this.config;
      this.mapa = L.map(this.contenedor().nativeElement).setView([centro.latitud, centro.longitud], zoom);
      L.tileLayer(teselas, { attribution: atribucion, maxZoom: 19 }).addTo(this.mapa);
      this.mapa.on('click', (e: L.LeafletMouseEvent) => this.marcarDesdeMapa(e.latlng));
      this.dibujar(this.ubicacion());
    });
    effect(() => this.dibujar(this.ubicacion()));
    inject(DestroyRef).onDestroy(() => this.mapa?.remove());
  }

  /** Coloca el marcador en el centro visible del mapa. */
  marcarCentro(): void {
    if (this.mapa) {
      this.marcarDesdeMapa(this.mapa.getCenter());
    }
  }

  private marcarDesdeMapa(punto: L.LatLng): void {
    this.cambioDesdeMapa = true;
    this.ubicacion.set({ latitud: punto.lat, longitud: punto.lng });
  }

  private dibujar(ubicacion: Ubicacion | null): void {
    const mapa = this.mapa;
    if (!mapa) {
      return;
    }
    if (!ubicacion) {
      this.marcador?.remove();
      this.marcador = undefined;
      return;
    }
    const punto = L.latLng(ubicacion.latitud, ubicacion.longitud);
    if (this.marcador) {
      this.marcador.setLatLng(punto);
    } else {
      this.marcador = L.marker(punto, {
        draggable: true,
        title: 'Ubicación del problema',
        alt: 'Ubicación del problema',
        icon: L.divIcon({ className: 'taan-marcador', iconSize: [26, 26], iconAnchor: [13, 26] }),
      }).addTo(mapa);
      this.marcador.on('dragend', () => this.marcarDesdeMapa(this.marcador!.getLatLng()));
    }
    if (!this.cambioDesdeMapa) {
      mapa.setView(punto, Math.max(mapa.getZoom(), ZOOM_UBICACION));
    }
    this.cambioDesdeMapa = false;
  }
}
