package mx.uv.taan.reportes.dominio;

import java.time.Instant;

import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Reporte ciudadano, núcleo del sistema (tabla reporte). Todo cambio de estado pasa por
 * {@link EstadoReporte#validarTransicion}: la entidad no permite saltarse el diagrama de estados.
 *
 * <p>La descripción del ciudadano nunca se sobrescribe. La ubicación es un punto PostGIS en
 * WGS84 (x = longitud, y = latitud). resultado_ia (jsonb) no se mapea aquí: se escribe por JDBC
 * en ResultadoIaRepository, para no depender de cómo Hibernate serializa JSON.</p>
 */
@Entity
@Table(name = "reporte")
public class Reporte {

    private static final GeometryFactory GEOMETRIA = new GeometryFactory(new PrecisionModel(), 4326);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private String folio;

    /** Nulo si la cuenta se eliminó: el reporte se conserva desvinculado (ON DELETE SET NULL). */
    @Column(name = "ciudadano_id", updatable = false)
    private Long ciudadanoId;

    @Column(nullable = false, updatable = false)
    private String descripcion;

    @Column(nullable = false, updatable = false, columnDefinition = "geometry(Point,4326)")
    private Point ubicacion;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, updatable = false, columnDefinition = "origen_reporte")
    private OrigenReporte origen;

    @Convert(converter = EstadoReporteConverter.class)
    @ColumnTransformer(write = "?::estado_reporte")
    @Column(nullable = false, columnDefinition = "estado_reporte")
    private EstadoReporte estado;

    /** Base del semáforo de antigüedad (RF-041). */
    @Column(name = "fecha_ultimo_estado", nullable = false)
    private Instant fechaUltimoEstado;

    @Column(name = "intentos_ia", nullable = false)
    private int intentosIa;

    @Column(name = "categoria_sugerida_id")
    private Long categoriaSugeridaId;

    @Column(name = "categoria_final_id")
    private Long categoriaFinalId;

    @Column(name = "posible_contenido_no_valido", nullable = false)
    private boolean posibleContenidoNoValido;

    @Column(name = "organizacion_id")
    private Long organizacionId;

    @Column(name = "area_id")
    private Long areaId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "motivo_canalizacion", columnDefinition = "motivo_canalizacion")
    private MotivoCanalizacion motivoCanalizacion;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "motivo_rechazo", columnDefinition = "motivo_rechazo")
    private MotivoRechazo motivoRechazo;

    @Column(name = "texto_rechazo_publico")
    private String textoRechazoPublico;

    @Column(name = "reporte_principal_id")
    private Long reportePrincipalId;

    /** Solo puede reabrirse una vez (RF-042). */
    @Column(nullable = false)
    private boolean reabierto;

    @Column(name = "fecha_resolucion")
    private Instant fechaResolucion;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    /** También la actualiza el disparador tg_reporte_fecha. */
    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    protected Reporte() {
        // Requerido por JPA.
    }

    /** Reporte recién enviado: queda en «Pendiente de canalización» («En revisión» para el ciudadano). */
    public Reporte(String folio, long ciudadanoId, String descripcion, double latitud, double longitud,
                   OrigenReporte origen) {
        Instant ahora = Instant.now();
        this.folio = folio;
        this.ciudadanoId = ciudadanoId;
        this.descripcion = descripcion;
        this.ubicacion = GEOMETRIA.createPoint(new Coordinate(longitud, latitud));
        this.origen = origen;
        this.estado = EstadoReporte.PENDIENTE_CANALIZACION;
        this.fechaUltimoEstado = ahora;
        this.creadoEn = ahora;
        this.actualizadoEn = ahora;
    }

    public void registrarIntentoIa() {
        this.intentosIa++;
    }

    /**
     * Resultado del procesamiento asíncrono (CU-01 paso 6): categoría propuesta por la IA y
     * organización/área decididas por el motor de reglas. Recibido para organizaciones
     * integradas; Canalización externa para las externas (RF-015).
     */
    public void canalizar(Long categoriaSugeridaId, boolean posibleContenidoNoValido, long organizacionId,
                          Long areaId, MotivoCanalizacion motivo, EstadoReporte destino) {
        estado.validarTransicion(destino);
        this.categoriaSugeridaId = categoriaSugeridaId;
        this.posibleContenidoNoValido = posibleContenidoNoValido;
        this.organizacionId = organizacionId;
        this.areaId = areaId;
        this.motivoCanalizacion = motivo;
        this.estado = destino;
        this.fechaUltimoEstado = Instant.now();
    }

    @PreUpdate
    void alActualizar() {
        this.actualizadoEn = Instant.now();
    }

    public double getLatitud() {
        return ubicacion.getY();
    }

    public double getLongitud() {
        return ubicacion.getX();
    }

    /** Categoría que ve el ciudadano: la confirmada por el personal o, si no hay, la propuesta. */
    public Long getCategoriaVisibleId() {
        return categoriaFinalId != null ? categoriaFinalId : categoriaSugeridaId;
    }

    public Long getId() {
        return id;
    }

    public String getFolio() {
        return folio;
    }

    public Long getCiudadanoId() {
        return ciudadanoId;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public OrigenReporte getOrigen() {
        return origen;
    }

    public EstadoReporte getEstado() {
        return estado;
    }

    public Instant getFechaUltimoEstado() {
        return fechaUltimoEstado;
    }

    public int getIntentosIa() {
        return intentosIa;
    }

    public Long getCategoriaSugeridaId() {
        return categoriaSugeridaId;
    }

    public Long getCategoriaFinalId() {
        return categoriaFinalId;
    }

    public boolean isPosibleContenidoNoValido() {
        return posibleContenidoNoValido;
    }

    public Long getOrganizacionId() {
        return organizacionId;
    }

    public Long getAreaId() {
        return areaId;
    }

    public MotivoCanalizacion getMotivoCanalizacion() {
        return motivoCanalizacion;
    }

    public MotivoRechazo getMotivoRechazo() {
        return motivoRechazo;
    }

    public String getTextoRechazoPublico() {
        return textoRechazoPublico;
    }

    public Long getReportePrincipalId() {
        return reportePrincipalId;
    }

    public boolean isReabierto() {
        return reabierto;
    }

    public Instant getFechaResolucion() {
        return fechaResolucion;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }
}
