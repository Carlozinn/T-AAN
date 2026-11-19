package mx.uv.taan.reportes.aplicacion;

import java.time.Instant;

/** Esquema ReporteResumenCiudadano del contrato: una fila de «Mis reportes» (RF-013, RF-040). */
public record ReporteResumenCiudadano(String folio, String estado, String estadoTexto, String categoria,
                                      Instant creadoEn, Instant actualizadoEn) {
}
