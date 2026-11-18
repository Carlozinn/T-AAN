package mx.uv.taan.configuracion.aplicacion;

import mx.uv.taan.configuracion.dominio.Area;

/** Esquema Area del contrato. telefonoPublico y correoPublico se envían como null si no hay. */
public record AreaVista(long id, String nombre, String telefonoPublico, String correoPublico, boolean activa) {

    static AreaVista de(Area a) {
        return new AreaVista(a.getId(), a.getNombre(), a.getTelefonoPublico(), a.getCorreoPublico(), a.isActiva());
    }
}
