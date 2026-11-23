package mx.uv.taan.ia;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tools.jackson.databind.ObjectMapper;

/**
 * Propone qué área atiende cada categoría a partir de las funciones de las áreas (CU-22 paso 3,
 * RF-039). Solo propone: nada se activa sin la confirmación de la coordinación (EX-03).
 */
public class ProponedorCorrespondencia {

    private final CadenaProveedores cadena;
    private final ObjectMapper json;

    ProponedorCorrespondencia(CadenaProveedores cadena, ObjectMapper json) {
        this.cadena = cadena;
        this.json = json;
    }

    /** funciones puede ser null: el área se envía solo con su nombre. */
    public record AreaParaIa(long id, String nombre, String funciones) {
    }

    /** nombre como lo ve la coordinación («Categoría» o «Categoría / Subcategoría»). */
    public record CategoriaParaIa(long id, String nombre) {
    }

    /**
     * @return categoriaId → areaId propuestos; solo pares con ids de las listas recibidas
     * @throws IaNoDisponibleException si no hay proveedor configurado o ninguno respondió
     */
    public Map<Long, Long> proponer(List<AreaParaIa> areas, List<CategoriaParaIa> categorias) {
        StringBuilder datos = new StringBuilder("Áreas:\n");
        areas.forEach(a -> datos.append("- id ").append(a.id()).append(": ").append(a.nombre())
                .append(a.funciones() == null ? "" : ". Funciones: " + a.funciones()).append('\n'));
        datos.append("\nCategorías:\n");
        categorias.forEach(c -> datos.append("- id ").append(c.id()).append(": ").append(c.nombre()).append('\n'));
        String texto = PromptsIa.delimitar(datos.toString());

        Set<Long> idsArea = areas.stream().map(AreaParaIa::id).collect(Collectors.toSet());
        Set<Long> idsCategoria = categorias.stream().map(CategoriaParaIa::id).collect(Collectors.toSet());
        return cadena.primeraRespuestaValida(p -> validar(
                        json.readValue(p.generarJson(PromptsIa.CORRESPONDENCIA, texto, PromptsIa.ESQUEMA_CORRESPONDENCIA),
                                Respuesta.class),
                        idsArea, idsCategoria))
                .orElseThrow(() -> new IaNoDisponibleException("No hay ningún servicio de IA configurado."));
    }

    /** Descarta ids que no se enviaron; si una categoría viene dos veces, cuenta la primera. */
    private static Map<Long, Long> validar(Respuesta r, Set<Long> idsArea, Set<Long> idsCategoria) {
        if (r == null || r.asignaciones() == null) {
            throw new IllegalStateException("Respuesta sin el formato pedido.");
        }
        Map<Long, Long> propuesta = new LinkedHashMap<>();
        for (Asignacion a : r.asignaciones()) {
            if (a != null && a.categoriaId() != null && a.areaId() != null
                    && idsCategoria.contains(a.categoriaId()) && idsArea.contains(a.areaId())) {
                propuesta.putIfAbsent(a.categoriaId(), a.areaId());
            }
        }
        return propuesta;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Respuesta(List<Asignacion> asignaciones) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Asignacion(Long categoriaId, Long areaId) {
    }
}
