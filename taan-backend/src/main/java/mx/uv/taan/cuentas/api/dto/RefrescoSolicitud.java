package mx.uv.taan.cuentas.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Esquema RefrescoSesion del contrato. */
public record RefrescoSolicitud(@NotBlank String refreshToken) {
}
