package mx.uv.taan.reportes.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Esquema Reapertura del contrato: por qué el problema persiste. */
public record ReaperturaSolicitud(@NotBlank @Size(min = 10, max = 1000) String motivo) {
}
