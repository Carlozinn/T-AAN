package mx.uv.taan.atencion.aplicacion;

import java.time.Instant;

/**
 * Esquema EventoHistorial del contrato: registro inmutable del historial (RF-017), con nombres
 * en lugar de ids. Incluye las notas internas que el ciudadano no ve (visibleCiudadano = false).
 */
public record EventoHistorial(
        Instant fecha,
        String accion,
        String estadoAnterior,
        String estadoNuevo,
        String organizacionAnterior,
        String organizacionNueva,
        String areaAnterior,
        String areaNueva,
        String observaciones,
        boolean visibleCiudadano,
        UsuarioRef usuario) {

    /** Esquema Usuarioref del contrato; null si actuó el sistema o el ciudadano. */
    public record UsuarioRef(long id, String nombre) {
    }
}
