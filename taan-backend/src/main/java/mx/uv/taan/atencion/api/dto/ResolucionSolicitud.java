package mx.uv.taan.atencion.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Esquema Resolucion del contrato. La observación de cierre la ve el ciudadano. */
public record ResolucionSolicitud(@NotBlank @Size(min = 5, max = 2000) String observacionCierre) {
}
