package mx.uv.taan.cuentas.aplicacion;

import java.time.Instant;

import mx.uv.taan.cuentas.dominio.Rol;
import mx.uv.taan.cuentas.dominio.Usuario;

/** Esquema UsuarioInstitucional del contrato (nunca incluye la contraseña ni su hash). */
public record UsuarioInstitucional(
        long id,
        String nombre,
        String correo,
        Rol rol,
        Long organizacionId,
        Long areaId,
        boolean activo,
        Instant ultimoAcceso) {

    static UsuarioInstitucional de(Usuario u) {
        return new UsuarioInstitucional(u.getId(), u.getNombre(), u.getCorreo(), u.getRol(),
                u.getOrganizacionId(), u.getAreaId(), u.isActivo(), u.getUltimoAcceso());
    }
}
