package mx.uv.taan.atencion.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Esquema SolicitudSenalamiento del contrato (CU-26 paso 1). */
public record SenalamientoSolicitud(@NotBlank @Size(min = 10, max = 2000) String motivo) {
}
