import { Injectable } from '@angular/core';
import { Geolocation } from '@capacitor/geolocation';

/** Ubicación del problema (RF-005): punto de partida del mapa, que el ciudadano puede ajustar. */
@Injectable({ providedIn: 'root' })
export class UbicacionService {
  async actual(): Promise<{ latitud: number; longitud: number }> {
    const posicion = await Geolocation.getCurrentPosition({ enableHighAccuracy: true, timeout: 10000 });
    return { latitud: posicion.coords.latitude, longitud: posicion.coords.longitude };
  }
}
