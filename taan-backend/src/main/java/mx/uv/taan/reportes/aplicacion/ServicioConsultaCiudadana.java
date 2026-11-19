package mx.uv.taan.reportes.aplicacion;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uv.taan.administracion.aplicacion.CanalExternoVista;
import mx.uv.taan.administracion.aplicacion.CategoriaVista;
import mx.uv.taan.administracion.aplicacion.OrganizacionVista;
import mx.uv.taan.administracion.aplicacion.ServicioCanalesExternos;
import mx.uv.taan.administracion.aplicacion.ServicioCategorias;
import mx.uv.taan.administracion.aplicacion.ServicioOrganizaciones;
import mx.uv.taan.archivos.aplicacion.ServicioArchivos;
import mx.uv.taan.comun.api.Pagina;
import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.configuracion.aplicacion.AreaVista;
import mx.uv.taan.configuracion.aplicacion.ServicioAreas;
import mx.uv.taan.reportes.dominio.EstadoReporte;
import mx.uv.taan.reportes.dominio.Reporte;
import mx.uv.taan.reportes.infraestructura.HistorialReporteRepository;
import mx.uv.taan.reportes.infraestructura.ReporteRepository;

/**
 * «Mis reportes» y su detalle (CU-02, CU-03; RF-013, RF-016, RF-040). Cada consulta va
 * filtrada por la cuenta: un folio de otra persona responde 404, sin revelar que existe.
 */
@Service
public class ServicioConsultaCiudadana {

    private static final int TAMANO_MAXIMO = 100;

    private final ReporteRepository reportes;
    private final HistorialReporteRepository historial;
    private final ServicioCategorias categorias;
    private final ServicioOrganizaciones organizaciones;
    private final ServicioAreas areas;
    private final ServicioCanalesExternos canales;
    private final ServicioArchivos archivos;

    public ServicioConsultaCiudadana(ReporteRepository reportes, HistorialReporteRepository historial,
                                     ServicioCategorias categorias, ServicioOrganizaciones organizaciones,
                                     ServicioAreas areas, ServicioCanalesExternos canales, ServicioArchivos archivos) {
        this.reportes = reportes;
        this.historial = historial;
        this.categorias = categorias;
        this.organizaciones = organizaciones;
        this.areas = areas;
        this.canales = canales;
        this.archivos = archivos;
    }

    @Transactional(readOnly = true)
    public Pagina<ReporteResumenCiudadano> listar(long ciudadanoId, int pagina, int tamano) {
        Map<Long, String> nombres = nombresCategoria();
        PageRequest solicitud = PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamano, 1), TAMANO_MAXIMO));
        return Pagina.de(reportes.findByCiudadanoIdOrderByCreadoEnDesc(ciudadanoId, solicitud),
                r -> new ReporteResumenCiudadano(r.getFolio(), r.getEstado().valorBd(), r.getEstado().textoCiudadano(),
                        nombres.get(r.getCategoriaVisibleId()), r.getCreadoEn(), r.getActualizadoEn()));
    }

    @Transactional(readOnly = true)
    public ReporteDetalleCiudadano detalle(long ciudadanoId, String folio) {
        Reporte r = reportes.findByFolioAndCiudadanoId(folio, ciudadanoId)
                .orElseThrow(ReglaNegocioException::noEncontrado);
        Optional<OrganizacionVista> organizacion = r.getOrganizacionId() == null
                ? Optional.empty() : organizaciones.consultar(r.getOrganizacionId());
        Optional<AreaVista> area = r.getAreaId() == null ? Optional.empty() : areas.buscar(r.getAreaId());
        EstadoReporte estado = r.getEstado();

        ReporteDetalleCiudadano.Rechazo rechazo = estado == EstadoReporte.RECHAZADO
                ? new ReporteDetalleCiudadano.Rechazo(r.getMotivoRechazo(), r.getTextoRechazoPublico(),
                        new ReporteDetalleCiudadano.ContactoArea(
                                area.map(AreaVista::telefonoPublico).orElse(null),
                                area.map(AreaVista::correoPublico).orElse(null)))
                : null;
        ReporteDetalleCiudadano.ReportePrincipal principal = estado == EstadoReporte.DUPLICADO
                && r.getReportePrincipalId() != null
                ? reportes.findById(r.getReportePrincipalId())
                        .map(p -> new ReporteDetalleCiudadano.ReportePrincipal(p.getFolio(), p.getEstado().valorBd(),
                                p.getEstado().textoCiudadano(), eventos(p.getId())))
                        .orElse(null)
                : null;
        List<CanalExternoVista> canalesExternos = estado == EstadoReporte.CANALIZACION_EXTERNA && r.getOrganizacionId() != null
                ? canales.listarActivosDe(r.getOrganizacionId())
                : null;

        return new ReporteDetalleCiudadano(r.getFolio(), estado.valorBd(), estado.textoCiudadano(),
                nombresCategoria().get(r.getCategoriaVisibleId()), r.getCreadoEn(), r.getActualizadoEn(),
                r.getDescripcion(), new ReporteDetalleCiudadano.Ubicacion(r.getLatitud(), r.getLongitud()),
                archivos.fotosCiudadanas(r.getId()),
                organizacion.map(OrganizacionVista::nombre).orElse(null),
                area.map(AreaVista::nombre).orElse(null),
                eventos(r.getId()), rechazo, principal, canalesExternos, reapertura(r, organizacion));
    }

    /** Para la descarga de archivos: ¿el reporte pertenece a esta cuenta? */
    @Transactional(readOnly = true)
    public boolean esDelCiudadano(long reporteId, long ciudadanoId) {
        return reportes.existsByIdAndCiudadanoId(reporteId, ciudadanoId);
    }

    /** Resuelto: puede reabrirse una sola vez dentro del plazo de su organización (RF-042). */
    private static ReporteDetalleCiudadano.Reapertura reapertura(Reporte r, Optional<OrganizacionVista> organizacion) {
        if (r.getEstado() != EstadoReporte.RESUELTO || r.getFechaResolucion() == null) {
            return null;
        }
        int plazo = organizacion.map(OrganizacionVista::plazoReaperturaDias).orElse(15);
        Instant vence = r.getFechaResolucion().plus(Duration.ofDays(plazo));
        return new ReporteDetalleCiudadano.Reapertura(!r.isReabierto() && Instant.now().isBefore(vence), vence);
    }

    private List<EventoPublico> eventos(long reporteId) {
        return historial.findByReporteIdAndVisibleCiudadanoTrueOrderByCreadoEnAscIdAsc(reporteId).stream()
                .filter(h -> h.getEstadoNuevo() != null)
                .map(EventoPublico::de)
                .toList();
    }

    private Map<Long, String> nombresCategoria() {
        return categorias.listar().stream().collect(Collectors.toMap(CategoriaVista::id, CategoriaVista::nombre));
    }
}
