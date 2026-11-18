package mx.uv.taan.cuentas.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Esquema Restablecimiento del contrato. */
public record RestablecimientoSolicitud(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 72) String nuevaPassword) {
}
