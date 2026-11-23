package mx.uv.taan.reportes.dominio;

import java.time.Instant;

import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Evento del historial de un reporte (tabla historial_reporte; RF-017, RNF-018). Solo se
 * insertan: {@code @Immutable} impide que Hibernate actualice un evento ya guardado.
 * usuario_id nulo significa que actuó el sistema o el ciudadano.
 */
@Entity
@Immutable
@Table(name = "historial_reporte")
public class HistorialReporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporte_id", nullable = false)
    private Long reporteId;

    @Convert(converter = EstadoReporteConverter.class)
    @ColumnTransformer(write = "?::estado_reporte")
    @Column(name = "estado_anterior", columnDefinition = "estado_reporte")
    private EstadoReporte estadoAnterior;

    @Convert(converter = EstadoReporteConverter.class)
    @ColumnTransformer(write = "?::estado_reporte")
    @Column(name = "estado_nuevo", columnDefinition = "estado_reporte")
    private EstadoReporte estadoNuevo;

    /** registro, canalizacion, validacion, asignacion, redireccion, resolucion, reapertura... */
    @Column(nullable = false)
    private String accion;

    @Column(name = "organizacion_anterior_id")
    private Long organizacionAnteriorId;

    @Column(name = "organizacion_nueva_id")
    private Long organizacionNuevaId;

    @Column(name = "area_anterior_id")
    private Long areaAnteriorId;

    @Column(name = "area_nueva_id")
    private Long areaNuevaId;

    /** Texto del evento; si visibleCiudadano, es lo que el ciudadano lee en su historial. */
    private String observaciones;

    @Column(name = "visible_ciudadano", nullable = false)
    private boolean visibleCiudadano;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected HistorialReporte() {
        // Requerido por JPA.
    }

    private HistorialReporte(Long reporteId, EstadoReporte estadoAnterior, EstadoReporte estadoNuevo,
                             String accion, String observaciones, boolean visibleCiudadano) {
        this.reporteId = reporteId;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.accion = accion;
        this.observaciones = observaciones;
        this.visibleCiudadano = visibleCiudadano;
        this.creadoEn = Instant.now();
    }

    /** El ciudadano envió el reporte (CU-01). */
    public static HistorialReporte registro(long reporteId) {
        return new HistorialReporte(reporteId, null, EstadoReporte.PENDIENTE_CANALIZACION, "registro",
                "Recibimos tu reporte. Lo estamos revisando para enviarlo a quien corresponde.", true);
    }

    /** El sistema canalizó el reporte con el motor de reglas (CU-01 paso 6). */
    public static HistorialReporte canalizacion(long reporteId, EstadoReporte estadoNuevo,
                                                long organizacionId, Long areaId, String observaciones) {
        HistorialReporte evento = new HistorialReporte(reporteId, EstadoReporte.PENDIENTE_CANALIZACION,
                estadoNuevo, "canalizacion", observaciones, true);
        evento.organizacionNuevaId = organizacionId;
        evento.areaNuevaId = areaId;
        return evento;
    }

    /**
     * Cambio de estado hecho por el personal (CU-06, CU-07, CU-08, CU-17). textoPublico es lo
     * que lee el ciudadano en su historial; lo interno va aparte, con {@link #nota}.
     */
    public static HistorialReporte cambioPorPersonal(long reporteId, EstadoReporte anterior, EstadoReporte nuevo,
                                                     String accion, String textoPublico, long usuarioId) {
        HistorialReporte evento = new HistorialReporte(reporteId, anterior, nuevo, accion, textoPublico, true);
        evento.usuarioId = usuarioId;
        return evento;
    }

    /** Igual que {@link #cambioPorPersonal}, registrando además el área anterior y la nueva. */
    public static HistorialReporte cambioDeArea(long reporteId, EstadoReporte anterior, String accion,
                                                Long areaAnterior, long areaNueva, String textoPublico, long usuarioId) {
        HistorialReporte evento = cambioPorPersonal(reporteId, anterior, EstadoReporte.ASIGNADO, accion,
                textoPublico, usuarioId);
        evento.areaAnteriorId = areaAnterior;
        evento.areaNuevaId = areaNueva;
        return evento;
    }

    /**
     * Nota interna sin cambio de estado (justificación de un rechazo o reasignación, corrección
     * de categoría, observaciones de un duplicado). Nunca la ve el ciudadano.
     */
    public static HistorialReporte nota(long reporteId, String accion, String texto, long usuarioId) {
        HistorialReporte evento = new HistorialReporte(reporteId, null, null, accion, texto, false);
        evento.usuarioId = usuarioId;
        return evento;
    }

    public Long getId() {
        return id;
    }

    public Long getOrganizacionAnteriorId() {
        return organizacionAnteriorId;
    }

    public Long getAreaAnteriorId() {
        return areaAnteriorId;
    }

    public Long getReporteId() {
        return reporteId;
    }

    public EstadoReporte getEstadoAnterior() {
        return estadoAnterior;
    }

    public EstadoReporte getEstadoNuevo() {
        return estadoNuevo;
    }

    public String getAccion() {
        return accion;
    }

    public Long getOrganizacionNuevaId() {
        return organizacionNuevaId;
    }

    public Long getAreaNuevaId() {
        return areaNuevaId;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public boolean isVisibleCiudadano() {
        return visibleCiudadano;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
