package mx.uv.taan.cuentas.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Esquema TokenVerificacion del contrato. */
public record TokenVerificacionSolicitud(@NotBlank String token) {
}
