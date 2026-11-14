package mx.uv.taan.cuentas.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Esquema CambioPassword del contrato (ciudadano e institucional). */
public record CambioPasswordSolicitud(
        @NotBlank String passwordActual,
        @NotBlank @Size(min = 8, max = 72) String nuevaPassword) {
}
