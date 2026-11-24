package mx.uv.taan.informes.api.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import mx.uv.taan.informes.dominio.FormatoInforme;

/** Esquema SolicitudInforme del contrato. areaId omitido: toda la organización. */
public record InformeSolicitud(@NotNull LocalDate periodoInicio, @NotNull LocalDate periodoFin, Long areaId,
                               @NotNull FormatoInforme formato) {
}
