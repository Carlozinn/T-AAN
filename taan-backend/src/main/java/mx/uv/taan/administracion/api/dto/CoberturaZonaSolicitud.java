package mx.uv.taan.administracion.api.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

/** Esquema CoberturaZona del contrato. La receptora por defecto se incluye siempre. */
public record CoberturaZonaSolicitud(@NotNull List<@NotNull Long> organizacionIds) {
}
