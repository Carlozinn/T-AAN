package mx.uv.taan.cuentas.aplicacion;

/**
 * Puerto de salida para validar el ID token de Google Sign-In (CU-24, CU-04). La
 * implementación verifica firma, vigencia, emisor y audiencia contra las llaves públicas
 * de Google; las pruebas usan una implementación falsa.
 */
public interface VerificadorGoogle {

    /**
     * Regresa la identidad contenida en un idToken válido y con correo verificado por Google.
     *
     * @throws mx.uv.taan.comun.error.ReglaNegocioException 400 TOKEN_GOOGLE_INVALIDO si el
     *         token no es válido, o 400 GOOGLE_NO_DISPONIBLE si falta configurar el cliente.
     */
    IdentidadGoogle verificar(String idToken);

    /** Identificador estable (sub), correo y nombre según Google. */
    record IdentidadGoogle(String sub, String correo, String nombre) {
    }
}
