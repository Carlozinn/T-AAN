package mx.uv.taan.notificaciones;

/**
 * Avisos al ciudadano en cada cambio de estado visible: push en la app (RF-034, Firebase
 * Cloud Messaging) y correo opcional (RF-044). Un fallo aquí nunca afecta al reporte.
 * PENDIENTE (incremento 7): implementaciones con Firebase Admin SDK y con el servicio de correo.
 */
public interface Notificador {

    void avisarCambioDeEstado(long ciudadanoId, String folio, String estadoTexto);
}
