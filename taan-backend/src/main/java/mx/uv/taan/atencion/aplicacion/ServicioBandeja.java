package mx.uv.taan.atencion.aplicacion;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uv.taan.administracion.aplicacion.CategoriaVista;
import mx.uv.taan.administracion.aplicacion.OrganizacionVista;
import mx.uv.taan.administracion.aplicacion.ServicioCategorias;
import mx.uv.taan.administracion.aplicacion.ServicioOrganizaciones;
import mx.uv.taan.archivos.aplicacion.ServicioArchivos;
import mx.uv.taan.atencion.dominio.Semaforo;
import mx.uv.taan.comun.api.Pagina;
import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.configuracion.aplicacion.AreaVista;
import mx.uv.taan.configuracion.aplicacion.ServicioAreas;
import mx.uv.taan.configuracion.aplicacion.ServicioSemaforo;
import mx.uv.taan.configuracion.aplicacion.TiempoObjetivo;
import mx.uv.taan.cuentas.aplicacion.ContactoCiudadano;
import mx.uv.taan.cuentas.aplicacion.ServicioCuentasInstitucionales;
import mx.uv.taan.cuentas.aplicacion.ServicioPerfilCiudadano;
import mx.uv.taan.cuentas.aplicacion.UsuarioActual;
import mx.uv.taan.cuentas.dominio.Rol;
import mx.uv.taan.reportes.aplicacion.FiltroBusqueda;
import mx.uv.taan.reportes.aplicacion.ServicioExpedientes;
import mx.uv.taan.reportes.dominio.EstadoReporte;
import mx.uv.taan.reportes.dominio.HistorialReporte;
import mx.uv.taan.reportes.dominio.Reporte;

/**
 * Bandeja, detalle e historial para el personal (CU-05, CU-09; RF-017, RF-018, RF-024, RF-041).
 *
 * <p>Alcance (RF-018, punto 5): el personal de área ve solo los reportes de su área; la
 * coordinación, todos los de su organización, incluidos los que aún no tienen área. Un reporte
 * fuera del alcance responde 404, sin revelar que existe.</p>
 */
@Service
public class ServicioBandeja {

    private static final int TAMANO_MAXIMO = 100;

    private final ServicioExpedientes expedientes;
    private final ServicioCategorias categorias;
    private final ServicioAreas areas;
    private final ServicioOrganizaciones organizaciones;
    private final ServicioSemaforo semaforo;
    private final ServicioArchivos archivos;
    private final ServicioPerfilCiudadano perfiles;
    private final ServicioCuentasInstitucionales cuentas;

    public ServicioBandeja(ServicioExpedientes expedientes, ServicioCategorias categorias, ServicioAreas areas,
                           ServicioOrganizaciones organizaciones, ServicioSemaforo semaforo, ServicioArchivos archivos,
                           ServicioPerfilCiudadano perfiles, ServicioCuentasInstitucionales cuentas) {
        this.expedientes = expedientes;
        this.categorias = categorias;
        this.areas = areas;
        this.organizaciones = organizaciones;
        this.semaforo = semaforo;
        this.archivos = archivos;
        this.perfiles = perfiles;
        this.cuentas = cuentas;
    }

    /** ¿Puede este usuario ver y operar este reporte? */
    public static boolean estaEnAlcance(UsuarioActual usuario, Reporte reporte) {
        if (reporte.getOrganizacionId() == null || !reporte.getOrganizacionId().equals(usuario.organizacionId())) {
            return false;
        }
        return usuario.rol() == Rol.coordinacion || Objects.equals(reporte.getAreaId(), usuario.areaId());
    }

    @Transactional(readOnly = true)
    public Pagina<ReporteResumenInstitucional> buscar(UsuarioActual usuario, List<String> estados, Long areaId,
                                                      Long categoriaId, String semaforoFiltro, LocalDate desde,
                                                      LocalDate hasta, String texto, int pagina, int tamano) {
        validarFiltros(estados, semaforoFiltro, texto, desde, hasta);
        FiltroBusqueda filtro = new FiltroBusqueda(usuario.organizacionId(),
                usuario.rol() == Rol.personal_area ? usuario.areaId() : null,
                estados, areaId, categoriaId, semaforoFiltro, desde, hasta, texto);
        int tamanoSeguro = Math.min(Math.max(tamano, 1), TAMANO_MAXIMO);
        Page<Reporte> resultado = expedientes.buscar(filtro, Math.max(pagina, 0), tamanoSeguro);

        Contexto contexto = contexto(usuario.organizacionId());
        Map<Long, Long> duplicados = expedientes.contarDuplicados(
                resultado.getContent().stream().map(Reporte::getId).toList());
        return Pagina.de(resultado, r -> resumen(r, contexto, duplicados.getOrDefault(r.getId(), 0L)));
    }

    @Transactional(readOnly = true)
    public ReporteDetalleInstitucional detalle(UsuarioActual usuario, String folio) {
        return armarDetalle(enAlcance(usuario, folio));
    }

    @Transactional(readOnly = true)
    public List<EventoHistorial> historial(UsuarioActual usuario, String folio) {
        return eventos(enAlcance(usuario, folio).getId());
    }

    /** Para la descarga de archivos: el personal solo ve los de reportes en su alcance. */
    @Transactional(readOnly = true)
    public boolean puedeVer(UsuarioActual usuario, long reporteId) {
        return expedientes.porId(reporteId).filter(r -> estaEnAlcance(usuario, r)).isPresent();
    }

    /**
     * Detalle completo de un reporte. Sin transacción propia: lo usan detalle() y, después de cada
     * cambio, el flujo de atención dentro de su transacción.
     */
    ReporteDetalleInstitucional armarDetalle(Reporte r) {
        Contexto contexto = contexto(r.getOrganizacionId());
        long duplicados = expedientes.contarDuplicados(List.of(r.getId())).getOrDefault(r.getId(), 0L);
        ReporteResumenInstitucional resumen = resumen(r, contexto, duplicados);
        ContactoCiudadano ciudadano = r.getCiudadanoId() == null ? null
                : perfiles.contacto(r.getCiudadanoId()).orElse(null);
        String principal = r.getReportePrincipalId() == null ? null
                : expedientes.porId(r.getReportePrincipalId()).map(Reporte::getFolio).orElse(null);
        String organizacion = organizaciones.consultar(r.getOrganizacionId()).map(OrganizacionVista::nombre).orElse(null);

        return new ReporteDetalleInstitucional(resumen.folio(), resumen.estado(), resumen.categoria(), resumen.area(),
                resumen.creadoEn(), resumen.fechaUltimoEstado(), resumen.semaforo(),
                resumen.posibleContenidoNoValido(), resumen.contadorDuplicados(), resumen.reabierto(),
                r.getDescripcion(), new ReporteDetalleInstitucional.Ubicacion(r.getLatitud(), r.getLongitud()),
                archivos.fotosCiudadanas(r.getId()), archivos.evidenciasAtencion(r.getId()), ciudadano,
                contexto.categorias().get(r.getCategoriaSugeridaId()), contexto.categorias().get(r.getCategoriaFinalId()),
                r.getMotivoCanalizacion(), organizacion, principal, eventos(r.getId()));
    }

    /** Busca por folio y verifica el alcance; fuera de él responde 404. */
    Reporte enAlcance(UsuarioActual usuario, String folio) {
        return expedientes.porFolio(folio)
                .filter(r -> estaEnAlcance(usuario, r))
                .orElseThrow(ReglaNegocioException::noEncontrado);
    }

    // ------------------------------------------------------------------ armado

    /** Nombres y tiempos que se reutilizan en toda una página. */
    private record Contexto(Map<Long, String> categorias, Map<Long, String> areas, Map<String, TiempoObjetivo> tiempos) {
    }

    private Contexto contexto(long organizacionId) {
        Map<Long, String> nombresCategoria = categorias.listar().stream()
                .collect(Collectors.toMap(CategoriaVista::id, CategoriaVista::nombre));
        Map<Long, String> nombresArea = areas.listar(organizacionId).stream()
                .collect(Collectors.toMap(AreaVista::id, AreaVista::nombre));
        return new Contexto(nombresCategoria, nombresArea, semaforo.tiemposDe(organizacionId));
    }

    private static ReporteResumenInstitucional resumen(Reporte r, Contexto c, long duplicados) {
        return new ReporteResumenInstitucional(r.getFolio(), r.getEstado().valorBd(),
                c.categorias().get(r.getCategoriaVisibleId()), c.areas().get(r.getAreaId()),
                r.getCreadoEn(), r.getFechaUltimoEstado(),
                Semaforo.calcular(c.tiempos().get(r.getEstado().valorBd()), r.getFechaUltimoEstado(), Instant.now()),
                r.isPosibleContenidoNoValido(), (int) duplicados, r.isReabierto());
    }

    private List<EventoHistorial> eventos(long reporteId) {
        List<HistorialReporte> historial = expedientes.historialCompleto(reporteId);
        Map<Long, String> usuarios = cuentas.nombres(historial.stream()
                .map(HistorialReporte::getUsuarioId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, String> nombresOrganizacion = organizaciones.nombres(historial.stream()
                .flatMap(h -> Stream.of(h.getOrganizacionAnteriorId(), h.getOrganizacionNuevaId()))
                .filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, String> nombresArea = areas.nombres(historial.stream()
                .flatMap(h -> Stream.of(h.getAreaAnteriorId(), h.getAreaNuevaId()))
                .filter(Objects::nonNull).collect(Collectors.toSet()));

        return historial.stream().map(h -> new EventoHistorial(h.getCreadoEn(), h.getAccion(),
                valor(h.getEstadoAnterior()), valor(h.getEstadoNuevo()),
                nombresOrganizacion.get(h.getOrganizacionAnteriorId()), nombresOrganizacion.get(h.getOrganizacionNuevaId()),
                nombresArea.get(h.getAreaAnteriorId()), nombresArea.get(h.getAreaNuevaId()),
                h.getObservaciones(), h.isVisibleCiudadano(),
                h.getUsuarioId() == null ? null
                        : new EventoHistorial.UsuarioRef(h.getUsuarioId(), usuarios.get(h.getUsuarioId()))))
                .toList();
    }

    private static String valor(EstadoReporte estado) {
        return estado == null ? null : estado.valorBd();
    }

    private static void validarFiltros(List<String> estados, String semaforo, String texto, LocalDate desde, LocalDate hasta) {
        if (estados != null) {
            for (String estado : estados) {
                try {
                    EstadoReporte.desdeValorBd(estado);
                } catch (IllegalArgumentException e) {
                    throw new ReglaNegocioException(400, "VALIDACION", "Estado desconocido: " + estado + ".");
                }
            }
        }
        if (semaforo != null && Semaforo.desdeTexto(semaforo) == null) {
            throw new ReglaNegocioException(400, "VALIDACION", "El semáforo debe ser verde, amarillo, rojo o sin_tiempo.");
        }
        if (texto != null && texto.length() > 100) {
            throw new ReglaNegocioException(400, "VALIDACION", "El texto de búsqueda admite hasta 100 caracteres.");
        }
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new ReglaNegocioException(400, "VALIDACION", "La fecha final no puede ser anterior a la inicial.");
        }
    }
}
