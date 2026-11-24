package mx.uv.taan.reportes.aplicacion;

/**
 * Novedad de un reporte que se avisa a su ciudadano (RF-034, RF-044). Se publica como evento de
 * Spring dentro de la transacción del cambio; el módulo de notificaciones la envía después de confirmarse.
 * resumen es el texto del aviso, sin datos sensibles.
 */
public record AvisoCiudadano(long ciudadanoId, String folio, String resumen) {
}
