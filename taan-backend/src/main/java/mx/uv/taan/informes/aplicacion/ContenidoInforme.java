package mx.uv.taan.informes.aplicacion;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import mx.uv.taan.reportes.aplicacion.IndicadoresReportes.Conteo;
import mx.uv.taan.reportes.dominio.EstadoReporte;

/**
 * Lo que se imprime en un informe. Ordena los datos en filas y secciones una sola vez, para
 * que el PDF y la hoja de cálculo muestren exactamente lo mismo.
 *
 * @param area null si el informe es de toda la organización
 */
public record ContenidoInforme(String organizacion, String area, Estadisticas estadisticas, Instant generadoEn) {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MOMENTO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("America/Mexico_City"));

    public record Fila(String etiqueta, String valor) {
    }

    public record Seccion(String titulo, List<Conteo> conteos) {
    }

    public String titulo() {
        return "Informe de reportes - " + organizacion;
    }

    public List<Fila> encabezado() {
        return List.of(
                new Fila("Ámbito", area == null ? "Toda la organización" : "Área: " + area),
                new Fila("Periodo", DIA.format(estadisticas.periodoInicio()) + " a " + DIA.format(estadisticas.periodoFin())),
                new Fila("Generado", MOMENTO.format(generadoEn)));
    }

    public List<Fila> indicadores() {
        Estadisticas e = estadisticas;
        return List.of(
                new Fila("Tiempo promedio hasta la asignación (horas)", horas(e.tiempoPromedioAsignacionHoras())),
                new Fila("Tiempo promedio hasta la resolución (horas)", horas(e.tiempoPromedioResolucionHoras())),
                new Fila("Reportes abiertos en tiempo", Long.toString(e.enTiempo())),
                new Fila("Reportes abiertos atrasados", Long.toString(e.atrasados())),
                new Fila("Reportes reabiertos", Long.toString(e.reabiertos())),
                new Fila("Reportes redirigidos a otra organización", Long.toString(e.redirigidos())));
    }

    public List<Seccion> secciones() {
        List<Conteo> estados = estadisticas.porEstado().stream()
                .map(c -> new Conteo(EstadoReporte.desdeValorBd(c.etiqueta()).textoCiudadano(), c.total())).toList();
        return List.of(new Seccion("Por estado", estados),
                new Seccion("Por categoría", estadisticas.porCategoria()),
                new Seccion("Por área", estadisticas.porArea()),
                new Seccion("Por zona", estadisticas.porZona()));
    }

    private static String horas(Double horas) {
        return horas == null ? "Sin datos" : String.format(Locale.ROOT, "%.1f", horas);
    }
}
