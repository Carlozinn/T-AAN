package mx.uv.taan.reportes.infraestructura;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Guarda la salida estructurada y validada de la IA en reporte.resultado_ia (jsonb). */
@Repository
public class ResultadoIaRepository {

    private final JdbcTemplate jdbc;

    public ResultadoIaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void guardar(long reporteId, String json) {
        jdbc.update("UPDATE reporte SET resultado_ia = ?::jsonb WHERE id = ?", json, reporteId);
    }
}
