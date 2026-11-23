package mx.uv.taan.configuracion.infraestructura;

import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import mx.uv.taan.configuracion.aplicacion.TiempoObjetivo;

/** Tiempos objetivo por estado de cada organización (tabla tiempo_objetivo; RF-041). */
@Repository
public class TiempoObjetivoRepository {

    private final JdbcTemplate jdbc;

    public TiempoObjetivoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Clave: valor del estado (recibido, asignado...). */
    public Map<String, TiempoObjetivo> deOrganizacion(long organizacionId) {
        Map<String, TiempoObjetivo> tiempos = new HashMap<>();
        jdbc.query("SELECT estado::text AS estado, horas_amarillo, horas_rojo FROM tiempo_objetivo WHERE organizacion_id = ?",
                fila -> {
                    tiempos.put(fila.getString("estado"),
                            new TiempoObjetivo(fila.getInt("horas_amarillo"), fila.getInt("horas_rojo")));
                }, organizacionId);
        return tiempos;
    }
}
