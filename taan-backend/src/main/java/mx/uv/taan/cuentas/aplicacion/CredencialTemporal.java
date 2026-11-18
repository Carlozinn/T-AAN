package mx.uv.taan.cuentas.aplicacion;

/**
 * Esquema CredencialTemporal del contrato. La contraseña temporal se muestra una sola vez
 * (no se guarda en claro en ningún lado) y debe cambiarse en el primer acceso (RF-003).
 */
public record CredencialTemporal(UsuarioInstitucional usuario, String passwordTemporal) {
}
