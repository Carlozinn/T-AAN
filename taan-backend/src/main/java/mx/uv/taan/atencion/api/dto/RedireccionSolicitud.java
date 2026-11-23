package mx.uv.taan.atencion.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Esquema Redireccion del contrato. categoriaFinalId opcional confirma o corrige la categoría,
 * igual que en la validación (CU-21 paso 3); la justificación es interna.
 */
public record RedireccionSolicitud(
        @NotNull Long organizacionDestinoId,
        Long categoriaFinalId,
        @NotBlank @Size(min = 10, max = 2000) String justificacion) {
}
