package mx.uv.taan.atencion.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mx.uv.taan.reportes.dominio.MotivoRechazo;

/** Esquema Rechazo del contrato. La justificación es interna; el ciudadano ve el motivo y el texto público. */
public record RechazoSolicitud(
        @NotNull MotivoRechazo motivoPublico,
        @Size(max = 500) String textoPublico,
        @NotBlank @Size(min = 10, max = 2000) String justificacion) {
}
