package mx.uv.taan.atencion.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Esquema Asignacion del contrato. La justificación es obligatoria al reasignar. */
public record AsignacionSolicitud(@NotNull Long areaId, @Size(max = 2000) String justificacion) {
}
