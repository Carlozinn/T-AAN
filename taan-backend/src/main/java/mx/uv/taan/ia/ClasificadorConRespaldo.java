package mx.uv.taan.ia;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tools.jackson.databind.ObjectMapper;

/**
 * Clasifica con el primer proveedor que responda algo válido (RF-007, RF-008). Una categoría que
 * no está en el catálogo es una respuesta inválida y pasa al respaldo: la IA nunca inventa
 * categorías. Si no hay ningún proveedor configurado, el reporte queda sin clasificación.
 */
class ClasificadorConRespaldo implements ClasificadorIa {

    private static final String SEPARADOR = " / ";

    private final CadenaProveedores cadena;
    private final ObjectMapper json;

    ClasificadorConRespaldo(CadenaProveedores cadena, ObjectMapper json) {
        this.cadena = cadena;
        this.json = json;
    }

    @Override
    public ResultadoClasificacion clasificar(String descripcion, List<String> categoriasPermitidas) {
        String datos = "Catálogo (una por línea; «Categoría / Subcategoría»):\n- "
                + String.join("\n- ", categoriasPermitidas)
                + "\n\nDescripción del ciudadano:\n" + PromptsIa.delimitar(descripcion);
        return cadena.primeraRespuestaValida(p -> validar(
                        json.readValue(p.generarJson(PromptsIa.CLASIFICACION, datos, PromptsIa.ESQUEMA_CLASIFICACION),
                                Respuesta.class),
                        categoriasPermitidas, p))
                .orElseGet(() -> ResultadoClasificacion.sinClasificacion(ResultadoClasificacion.SIN_PROVEEDOR));
    }

    /** Devuelve los nombres tal como están en el catálogo; una subcategoría ajena se descarta. */
    private static ResultadoClasificacion validar(Respuesta r, List<String> catalogo, ProveedorIa p) {
        if (r == null || r.posibleContenidoNoValido() == null) {
            throw new IllegalStateException("Respuesta sin el formato pedido.");
        }
        boolean contenido = r.posibleContenidoNoValido();
        String motivo = contenido ? vacioANulo(r.motivoContenido()) : null;
        String categoria = vacioANulo(r.categoria());
        if (categoria == null) {
            return new ResultadoClasificacion(null, null, contenido, motivo, p.nombre(), p.modelo(),
                    ResultadoClasificacion.SIN_CATEGORIA);
        }
        Map<String, String> porNombre = new LinkedHashMap<>();
        catalogo.forEach(c -> porNombre.put(c.toLowerCase(), c));
        String raiz = porNombre.get(categoria.toLowerCase());
        if (raiz == null || raiz.contains(SEPARADOR)) {
            throw new IllegalStateException("La categoría propuesta no está en el catálogo.");
        }
        String subcategoria = vacioANulo(r.subcategoria());
        String completa = subcategoria == null ? null : porNombre.get((raiz + SEPARADOR + subcategoria).toLowerCase());
        return new ResultadoClasificacion(raiz,
                completa == null ? null : completa.substring(raiz.length() + SEPARADOR.length()),
                contenido, motivo, p.nombre(), p.modelo(), null);
    }

    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Respuesta(String categoria, String subcategoria, Boolean posibleContenidoNoValido, String motivoContenido) {
    }
}
