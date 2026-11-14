import { Injectable } from '@angular/core';
import { Camera, CameraResultType, CameraSource } from '@capacitor/camera';

/**
 * Fotografía opcional del reporte (RF-006). Devuelve el archivo listo para enviarse
 * en el formulario multipart de POST /ciudadano/reportes. Los metadatos GPS los
 * elimina el backend (RNF-017).
 */
@Injectable({ providedIn: 'root' })
export class CamaraService {
  async tomarOElegir(): Promise<Blob | null> {
    const foto = await Camera.getPhoto({
      quality: 80,
      resultType: CameraResultType.Uri,
      source: CameraSource.Prompt, // el ciudadano elige cámara o galería
    });
    if (!foto.webPath) {
      return null;
    }
    const respuesta = await fetch(foto.webPath);
    return respuesta.blob();
  }
}
