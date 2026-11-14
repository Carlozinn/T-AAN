package mx.uv.taan.comun.seguridad;

/**
 * Nombres de los claims y tipos de token que emite el backend (RF-002). Los usa la
 * configuración de seguridad (para aceptar solo tokens de acceso en la API) y el emisor
 * de tokens del módulo cuentas.
 */
public final class JwtTaan {

    /** Claim con el rol del contrato: ciudadano, personal_area, coordinacion o administrador. */
    public static final String CLAIM_ROL = "rol";

    /** Claim que distingue para qué sirve el token; evita usar un refresco como acceso. */
    public static final String CLAIM_TIPO = "tipo";

    /** Autentica las peticiones («Authorization: Bearer»); vive pocos minutos. */
    public static final String TIPO_ACCESO = "acceso";

    /** Renueva la sesión en POST /auth/refresco; vive días. */
    public static final String TIPO_REFRESCO = "refresco";

    /** Confirma el correo de una cuenta nueva (CU-24); viaja en el enlace del correo. */
    public static final String TIPO_VERIFICACION = "verificacion";

    private JwtTaan() {
    }
}
