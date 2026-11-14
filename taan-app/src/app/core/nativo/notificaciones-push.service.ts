import { Injectable } from '@angular/core';
import { Capacitor } from '@capacitor/core';
import { PushNotifications } from '@capacitor/push-notifications';

/**
 * Registra el celular en Firebase Cloud Messaging (RF-034).
 * El token obtenido se envía al backend con POST /ciudadano/dispositivos.
 */
@Injectable({ providedIn: 'root' })
export class NotificacionesPushService {
  async registrar(alObtenerToken: (token: string) => void): Promise<void> {
    if (!Capacitor.isNativePlatform()) {
      return; // en el navegador no hay notificaciones push
    }
    const permiso = await PushNotifications.requestPermissions();
    if (permiso.receive !== 'granted') {
      return; // el ciudadano no dio permiso: la app sigue funcionando sin avisos
    }
    await PushNotifications.addListener('registration', (token) => alObtenerToken(token.value));
    await PushNotifications.register();
  }
}
