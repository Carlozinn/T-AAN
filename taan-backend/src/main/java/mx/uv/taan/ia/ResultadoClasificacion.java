package mx.uv.taan.ia;

/**
 * Salida de la IA ya validada contra el catálogo. La IA solo PROPONE la clasificación; la
 * organización la decide el motor de reglas.
 *
 * @param categoria                nombre de la categoría del catálogo, o null si no corresponde a ninguna
 * @param subcategoria             nombre de la subcategoría, o null
 * @param posibleContenidoNoValido marca informativa (RF-007 punto 6); no rechaza el reporte
 * @param motivoContenido          por qué se marcó, en una frase; null si no se marcó
 * @param proveedor                quién respondió (gemini, groq); null si no hubo respuesta de IA
 * @param modelo                   modelo que respondió; null si no hubo respuesta de IA
 * @param razon                    por qué no hay categoría (constantes SIN_*); null si se clasificó
 */
public record ResultadoClasificacion(String categoria, String subcategoria, boolean posibleContenidoNoValido,
                                     String motivoContenido, String proveedor, String modelo, String razon) {

    /** La IA respondió, pero ninguna categoría del catálogo corresponde o la descripción no basta. */
    public static final String SIN_CATEGORIA = "sin_categoria";
    /** No hay ningún proveedor configurado (entorno sin claves). */
    public static final String SIN_PROVEEDOR = "sin_proveedor";
    /** Los proveedores fallaron en todos los intentos permitidos. */
    public static final String IA_NO_DISPONIBLE = "ia_no_disponible";

    public static ResultadoClasificacion sinClasificacion(String razon) {
        return new ResultadoClasificacion(null, null, false, null, null, null, razon);
    }

    public boolean clasificado() {
        return categoria != null;
    }
}
