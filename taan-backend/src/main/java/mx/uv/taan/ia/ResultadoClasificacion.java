package mx.uv.taan.ia;

/**
 * Salida estructurada de la IA ya validada por el backend.
 * La IA solo PROPONE la clasificación; la organización la decide el motor de reglas.
 *
 * @param categoria                nombre de la categoría del catálogo, o null si no corresponde a ninguna
 * @param subcategoria             nombre de la subcategoría, o null
 * @param posibleContenidoNoValido marca informativa (RF-008); no rechaza el reporte
 */
public record ResultadoClasificacion(String categoria, String subcategoria, boolean posibleContenidoNoValido) {

    public static ResultadoClasificacion sinClasificacion() {
        return new ResultadoClasificacion(null, null, false);
    }

    public boolean clasificado() {
        return categoria != null;
    }
}
