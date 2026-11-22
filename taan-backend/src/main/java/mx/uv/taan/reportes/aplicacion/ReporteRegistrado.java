package mx.uv.taan.reportes.aplicacion;

/**
 * Evento que se publica al guardar un reporte. ProcesadorReportes lo escucha después de que
 * la transacción se confirma y arranca el procesamiento asíncrono (IA y reglas).
 */
public record ReporteRegistrado(long reporteId) {
}
