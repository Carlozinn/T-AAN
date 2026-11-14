package mx.uv.taan.cuentas.dominio;

/**
 * Estado de la cuenta ciudadana (RF-043). En minúsculas para coincidir con las etiquetas
 * del tipo {@code estado_cuenta} de PostgreSQL.
 *
 * <p>Una cuenta suspendida sigue entrando y consulta «Mis reportes», pero no crea ni reabre
 * reportes: esa regla se aplica en el módulo de reportes, no al iniciar sesión.</p>
 */
public enum EstadoCuenta {

    activa,
    suspendida
}
