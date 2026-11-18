package mx.uv.taan.administracion.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import mx.uv.taan.administracion.dominio.NivelParticipacion;

/** Esquema OrganizacionActualizacion del contrato: campos opcionales (null = sin cambio). */
public record OrganizacionActualizacionSolicitud(
        @Size(min = 2, max = 200) @Pattern(regexp = ".*\\S.*", message = "no puede estar vacío") String nombre,
        NivelParticipacion nivelParticipacion,
        @Min(1) Integer plazoReaperturaDias,
        Boolean activa) {
}
