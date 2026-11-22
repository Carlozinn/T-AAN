package mx.uv.taan.atencion.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Esquema MarcaDuplicado del contrato. */
public record MarcaDuplicadoSolicitud(
        @NotBlank @Pattern(regexp = "^[A-Z0-9-]{6,20}$", message = "no es un folio válido") String folioPrincipal,
        @NotBlank @Size(min = 5, max = 2000) String observaciones) {
}
