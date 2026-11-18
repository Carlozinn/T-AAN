package mx.uv.taan.administracion.api.dto;

import java.time.LocalDate;

/** Esquema ReglaActualizacion del contrato: campos opcionales (null = sin cambio). */
public record ReglaActualizacionSolicitud(
        Integer prioridad,
        Long areaId,
        LocalDate vigenteHasta,
        Boolean activa) {
}
