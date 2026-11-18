package mx.uv.taan.configuracion.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** Esquema ConfirmacionCorrespondencia del contrato. areaId null deja la categoría sin área. */
public record ConfirmacionCorrespondenciaSolicitud(@NotEmpty List<@Valid Elemento> elementos) {

    public record Elemento(@NotNull Long categoriaId, Long areaId) {
    }
}
