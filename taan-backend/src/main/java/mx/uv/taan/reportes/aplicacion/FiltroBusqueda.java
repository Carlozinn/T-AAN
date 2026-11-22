package mx.uv.taan.reportes.aplicacion;

import java.time.LocalDate;
import java.util.List;

/**
 * Filtros de la bandeja institucional (CU-05). organizacionId y areaAlcance los pone el
 * servidor según quién consulta (no el cliente): son la segunda barrera de autorización.
 *
 * @param areaAlcance área del personal de área; null para la coordinación (ve toda la organización)
 * @param estados     valores de estado_reporte (recibido, asignado...)
 * @param semaforo    verde, amarillo, rojo o sin_tiempo
 */
public record FiltroBusqueda(long organizacionId, Long areaAlcance, List<String> estados, Long areaId,
                             Long categoriaId, String semaforo, LocalDate desde, LocalDate hasta, String texto) {
}
