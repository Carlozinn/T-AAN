package mx.uv.taan.reportes.aplicacion;

import java.time.Instant;

import mx.uv.taan.reportes.dominio.HistorialReporte;

/** Esquema EventoPublico del contrato: un cambio del historial visible para el ciudadano. */
public record EventoPublico(Instant fecha, String estado, String estadoTexto, String descripcion) {

    static EventoPublico de(HistorialReporte h) {
        return new EventoPublico(h.getCreadoEn(), h.getEstadoNuevo().valorBd(),
                h.getEstadoNuevo().textoCiudadano(), h.getObservaciones());
    }
}
