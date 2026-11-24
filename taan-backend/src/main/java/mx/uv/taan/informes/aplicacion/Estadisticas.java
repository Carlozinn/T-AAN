package mx.uv.taan.informes.aplicacion;

import java.time.LocalDate;
import java.util.List;

import mx.uv.taan.reportes.aplicacion.IndicadoresReportes.Conteo;

/** Esquema Estadisticas del contrato (CU-10). Sin datos personales de ciudadanos (RF-026). */
public record Estadisticas(LocalDate periodoInicio, LocalDate periodoFin, List<Conteo> porEstado,
                           List<Conteo> porCategoria, List<Conteo> porArea, List<Conteo> porZona,
                           Double tiempoPromedioAsignacionHoras, Double tiempoPromedioResolucionHoras,
                           long enTiempo, long atrasados, long reabiertos, long redirigidos) {
}
