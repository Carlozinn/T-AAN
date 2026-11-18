package mx.uv.taan.administracion.aplicacion;

import mx.uv.taan.administracion.dominio.CanalExterno;
import mx.uv.taan.administracion.dominio.TipoCanal;

/** Esquema CanalExterno del contrato (incluye el nombre de la organización). */
public record CanalExternoVista(long id, long organizacionId, String organizacion, TipoCanal tipo,
                                String valor, String fuenteVerificacion, boolean activo) {

    static CanalExternoVista de(CanalExterno c, String organizacion) {
        return new CanalExternoVista(c.getId(), c.getOrganizacionId(), organizacion, c.getTipo(),
                c.getValor(), c.getFuenteVerificacion(), c.isActivo());
    }
}
