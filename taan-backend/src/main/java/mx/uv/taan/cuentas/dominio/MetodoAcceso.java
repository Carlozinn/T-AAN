package mx.uv.taan.cuentas.dominio;

/**
 * Cómo entra el ciudadano a su cuenta (RF-001, RF-002). En minúsculas para coincidir con
 * las etiquetas del tipo {@code metodo_acceso} de PostgreSQL.
 */
public enum MetodoAcceso {

    /** Google Sign-In: la cuenta guarda google_sub y no tiene contraseña. */
    google,

    /** Correo y contraseña: requiere confirmar el enlace de verificación para reportar. */
    correo
}
