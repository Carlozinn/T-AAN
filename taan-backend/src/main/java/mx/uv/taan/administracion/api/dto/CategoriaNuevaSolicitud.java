package mx.uv.taan.administracion.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Esquema CategoriaNueva del contrato: sin categoriaPadreId es una categoría raíz. */
public record CategoriaNuevaSolicitud(
        @NotBlank @Size(min = 2, max = 150) String nombre,
        Long categoriaPadreId) {
}
