package mx.uv.taan.reportes.aplicacion;

import java.util.List;

/**
 * Indicadores agregados de los reportes creados en un periodo (RF-026, RF-041, RF-042).
 * Solo conteos y promedios: ningún dato personal.
 *
 * @param porEstado        etiqueta = valor del estado en minúsculas (recibido, asignado...)
 * @param horasAsignacion  promedio desde la creación hasta la primera asignación; null sin datos
 * @param horasResolucion  promedio desde la creación hasta la resolución; null sin datos
 * @param enTiempo         reportes abiertos con semáforo verde o amarillo
 * @param atrasados        reportes abiertos con semáforo rojo
 * @param redirigidos      reportes que la organización (o el área) envió a otra organización
 */
public record IndicadoresReportes(List<Conteo> porEstado, List<Conteo> porCategoria, List<Conteo> porArea,
                                  List<Conteo> porZona, Double horasAsignacion, Double horasResolucion,
                                  long enTiempo, long atrasados, long reabiertos, long redirigidos) {

    public record Conteo(String etiqueta, long total) {
    }
}
