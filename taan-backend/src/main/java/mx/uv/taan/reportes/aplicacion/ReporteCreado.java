package mx.uv.taan.reportes.aplicacion;

import java.time.Instant;

/** Esquema ReporteCreado del contrato: lo que ve el ciudadano al enviar (folio y «En revisión»). */
public record ReporteCreado(String folio, String estado, String estadoTexto, Instant creadoEn) {
}
