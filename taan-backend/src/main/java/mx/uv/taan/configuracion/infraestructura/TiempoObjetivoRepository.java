package mx.uv.taan.configuracion.infraestructura;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import mx.uv.taan.configuracion.aplicacion.TiempoObjetivo;
import mx.uv.taan.configuracion.aplicacion.TiempoObjetivoEstado;

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

    /**
     * En el orden del tipo estado_reporte, que es el del flujo (recibido, validado, asignado,
     * en_atencion, reabierto). Se ordena por la columna calificada: «ORDER BY estado» tomaría el
     * alias de texto y ordenaría alfabéticamente.
     */
    public List<TiempoObjetivoEstado> listar(long organizacionId) {
        return jdbc.query("""
                SELECT estado::text AS estado, horas_amarillo, horas_rojo FROM tiempo_objetivo
                WHERE organizacion_id = ? ORDER BY tiempo_objetivo.estado""",
                (fila, n) -> new TiempoObjetivoEstado(fila.getString("estado"), fila.getInt("horas_amarillo"),
                        fila.getInt("horas_rojo")),
                organizacionId);
    }

    /** Reemplaza toda la configuración de la organización; requiere la transacción de quien llama. */
    public void reemplazar(long organizacionId, List<TiempoObjetivoEstado> tiempos) {
        jdbc.update("DELETE FROM tiempo_objetivo WHERE organizacion_id = ?", organizacionId);
        jdbc.batchUpdate("""
                INSERT INTO tiempo_objetivo (organizacion_id, estado, horas_amarillo, horas_rojo)
                VALUES (?, ?::estado_reporte, ?, ?)""",
                tiempos.stream()
                        .map(t -> new Object[] {organizacionId, t.estado(), t.horasAmarillo(), t.horasRojo()})
                        .toList());
    }
}
