package mx.uv.taan.notificaciones;

/**
 * Avisos al ciudadano en cada novedad visible de sus reportes: push en la app (RF-034, Firebase
 * Cloud Messaging) y correo opcional (RF-044). Un fallo aquí nunca afecta al reporte.
 */
public interface Notificador {

    /** resumen es el texto del aviso (folio y estado u organización), sin datos sensibles. */
    void avisar(long ciudadanoId, String folio, String resumen);
}
