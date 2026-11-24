package mx.uv.taan.reportes.aplicacion;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.reportes.dominio.HistorialReporte;
import mx.uv.taan.reportes.dominio.Reporte;
import mx.uv.taan.reportes.infraestructura.BusquedaReportes;
import mx.uv.taan.reportes.infraestructura.EstadisticasReportes;
import mx.uv.taan.reportes.infraestructura.HistorialReporteRepository;
import mx.uv.taan.reportes.infraestructura.ReporteRepository;

/**
 * Puerta del módulo de reportes para los demás módulos (por ahora, atención institucional).
 * Los repositorios de reportes no salen de este módulo: quien necesite leer, cambiar o
 * historiar un reporte lo hace por aquí.
 *
 * <p>Entrega entidades {@link Reporte} porque sus cambios de estado viven en la propia entidad
 * y la validan contra el diagrama de estados: quien la recibe no puede saltarse una regla.
 * Los métodos que bloquean o guardan exigen una transacción abierta por quien llama
 * (Propagation.MANDATORY), para que el cambio y su historial se confirmen juntos.</p>
 */
@Service
public class ServicioExpedientes {

    private final ReporteRepository reportes;
    private final HistorialReporteRepository historial;
    private final BusquedaReportes busqueda;
    private final EstadisticasReportes estadisticas;

    public ServicioExpedientes(ReporteRepository reportes, HistorialReporteRepository historial,
                               BusquedaReportes busqueda, EstadisticasReportes estadisticas) {
        this.reportes = reportes;
        this.historial = historial;
        this.busqueda = busqueda;
        this.estadisticas = estadisticas;
    }

    @Transactional(readOnly = true)
    public Optional<Reporte> porFolio(String folio) {
        return reportes.findByFolio(folio);
    }

    @Transactional(readOnly = true)
    public Optional<Reporte> porId(long id) {
        return reportes.findById(id);
    }

    /** Varios reportes en una sola consulta, indexados por id. */
    @Transactional(readOnly = true)
    public Map<Long, Reporte> porIds(Collection<Long> ids) {
        return reportes.findAllById(ids).stream().collect(Collectors.toMap(Reporte::getId, Function.identity()));
    }

    /** Indicadores agregados del periodo para estadísticas e informes (CU-10); sin datos personales. */
    @Transactional(readOnly = true)
    public IndicadoresReportes indicadores(FiltroIndicadores filtro) {
        return estadisticas.calcular(filtro);
    }

    /** Bloquea la fila hasta el fin de la transacción de quien llama (SELECT ... FOR UPDATE). */
    @Transactional(propagation = Propagation.MANDATORY)
    public Reporte bloquearPorFolio(String folio) {
        return reportes.bloquearPorFolio(folio).orElseThrow(ReglaNegocioException::noEncontrado);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(HistorialReporte... eventos) {
        historial.saveAll(List.of(eventos));
    }

    /** Página de la bandeja, en el orden de la búsqueda. */
    @Transactional(readOnly = true)
    public PageImpl<Reporte> buscar(FiltroBusqueda filtro, int pagina, int tamano) {
        BusquedaReportes.PaginaIds resultado = busqueda.buscar(filtro, pagina, tamano);
        Map<Long, Reporte> porId = reportes.findAllById(resultado.ids()).stream()
                .collect(Collectors.toMap(Reporte::getId, Function.identity()));
        List<Reporte> ordenados = resultado.ids().stream().map(porId::get).toList();
        return new PageImpl<>(ordenados, PageRequest.of(pagina, tamano), resultado.total());
    }

    @Transactional(readOnly = true)
    public List<HistorialReporte> historialCompleto(long reporteId) {
        return historial.findByReporteIdOrderByCreadoEnAscIdAsc(reporteId);
    }

    /** Cuántos reportes apuntan a cada uno como principal («Reportado por N personas»). */
    @Transactional(readOnly = true)
    public Map<Long, Long> contarDuplicados(Collection<Long> ids) {
        Map<Long, Long> conteo = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] fila : reportes.contarDuplicados(ids)) {
                conteo.put((Long) fila[0], (Long) fila[1]);
            }
        }
        return conteo;
    }
}
