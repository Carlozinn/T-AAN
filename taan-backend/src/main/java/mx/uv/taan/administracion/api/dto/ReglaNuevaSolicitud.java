package mx.uv.taan.administracion.api.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

/** Esquema ReglaNueva del contrato; prioridad 0 y vigencia desde hoy si no se envían. */
public record ReglaNuevaSolicitud(
        @NotNull Long zonaId,
        @NotNull Long categoriaId,
        @NotNull Long organizacionId,
        Long areaId,
        Integer prioridad,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta) {
}
