package mx.uv.taan.cuentas.dominio;

/**
 * Roles del contrato (esquema Rol de TAAN_API.yaml). El rol viaja en el claim "rol" del JWT
 * y define la primera barrera de autorización (ConfiguracionSeguridad).
 *
 * <p>Los nombres van en minúsculas a propósito: así {@code name()} coincide a la vez con las
 * etiquetas del tipo {@code rol_usuario} de PostgreSQL, con el JSON del contrato y con las
 * autoridades {@code ROLE_<rol>} de Spring Security, sin conversiones intermedias.</p>
 */
public enum Rol {

    ciudadano,
    personal_area,
    coordinacion,
    administrador;

    /** Roles del personal institucional (los que viven en la tabla usuario). */
    public boolean esInstitucional() {
        return this != ciudadano;
    }
}
