package mx.uv.taan.administracion.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mx.uv.taan.administracion.dominio.NivelParticipacion;

/** Esquema OrganizacionNueva del contrato; plazoReaperturaDias es 15 si no se envía. */
public record OrganizacionNuevaSolicitud(
        @NotBlank @Size(min = 2, max = 200) String nombre,
        @NotNull NivelParticipacion nivelParticipacion,
        @Min(1) Integer plazoReaperturaDias) {
}
