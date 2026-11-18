package mx.uv.taan.administracion.aplicacion;

import mx.uv.taan.administracion.dominio.NivelParticipacion;
import mx.uv.taan.administracion.dominio.Organizacion;

/** Esquema Organizacion del contrato. */
public record OrganizacionVista(long id, String nombre, NivelParticipacion nivelParticipacion,
                                int plazoReaperturaDias, boolean activa) {

    static OrganizacionVista de(Organizacion o) {
        return new OrganizacionVista(o.getId(), o.getNombre(), o.getNivelParticipacion(),
                o.getPlazoReaperturaDias(), o.isActiva());
    }
}
