package mx.uv.taan.configuracion.infraestructura;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import mx.uv.taan.configuracion.aplicacion.PlantillaAreaVista;

/**
 * Lectura de la plantilla municipal (tablas plantilla_area y plantilla_correspondencia; RF-039).
 * Es un catálogo de solo lectura que se carga en la migración, por eso basta con JDBC.
 */
@Repository
public class PlantillaRepository {

    private final JdbcTemplate jdbc;

    public PlantillaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Cada área estándar con los nombres de las categorías que atiende. */
    public List<PlantillaAreaVista> consultar() {
        Map<String, String> descripciones = new LinkedHashMap<>();
        Map<String, List<String>> categorias = new LinkedHashMap<>();
        jdbc.query("""
                SELECT pa.nombre, pa.descripcion, c.nombre AS categoria
                FROM plantilla_area pa
                LEFT JOIN plantilla_correspondencia pc ON pc.plantilla_area_id = pa.id
                LEFT JOIN categoria c ON c.id = pc.categoria_id
                ORDER BY pa.nombre, c.nombre""", fila -> {
            String area = fila.getString("nombre");
            descripciones.putIfAbsent(area, fila.getString("descripcion"));
            List<String> deEstaArea = categorias.computeIfAbsent(area, a -> new ArrayList<>());
            String categoria = fila.getString("categoria");
            if (categoria != null) {
                deEstaArea.add(categoria);
            }
        });
        return descripciones.entrySet().stream()
                .map(e -> new PlantillaAreaVista(e.getKey(), e.getValue(), categorias.get(e.getKey())))
                .toList();
    }
}
