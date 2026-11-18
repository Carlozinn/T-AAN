package mx.uv.taan.administracion.aplicacion;

import java.time.LocalDate;

import mx.uv.taan.administracion.dominio.Regla;

/** Esquema Regla del contrato. */
public record ReglaVista(long id, long zonaId, long categoriaId, long organizacionId, Long areaId,
                         int prioridad, LocalDate vigenteDesde, LocalDate vigenteHasta, boolean activa) {

    static ReglaVista de(Regla r) {
        return new ReglaVista(r.getId(), r.getZonaId(), r.getCategoriaId(), r.getOrganizacionId(), r.getAreaId(),
                r.getPrioridad(), r.getVigenteDesde(), r.getVigenteHasta(), r.isActiva());
    }
}
