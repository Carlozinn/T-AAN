package mx.uv.taan.atencion.aplicacion;

import java.time.Instant;

import mx.uv.taan.atencion.dominio.Semaforo;

/** Esquema ReporteResumenInstitucional del contrato: una fila de la bandeja (CU-05). */
public record ReporteResumenInstitucional(
        String folio,
        String estado,
        String categoria,
        String area,
        Instant creadoEn,
        Instant fechaUltimoEstado,
        Semaforo semaforo,
        boolean posibleContenidoNoValido,
        int contadorDuplicados,
        boolean reabierto) {
}
