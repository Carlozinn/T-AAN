package mx.uv.taan.atencion.dominio;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import mx.uv.taan.comun.error.ReglaNegocioException;

/**
 * Señalamiento de una cuenta por posible abuso (tabla senalamiento_cuenta; RF-043, CU-26).
 * Es el registro de auditoría de la moderación: se crea pendiente y el administrador lo
 * resuelve una sola vez. La suspensión vigente se guarda en la cuenta ciudadana.
 */
@Entity
@Table(name = "senalamiento_cuenta")
public class Senalamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporte_id", nullable = false, updatable = false)
    private Long reporteId;

    /** Nulo si la cuenta se eliminó después del señalamiento. */
    @Column(name = "ciudadano_id")
    private Long ciudadanoId;

    @Column(name = "senalado_por", nullable = false, updatable = false)
    private Long senaladoPor;

    @Column(nullable = false, updatable = false)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "estado_senalamiento")
    private EstadoSenalamiento estado = EstadoSenalamiento.pendiente;

    @Column(name = "resuelto_por")
    private Long resueltoPor;

    private String resolucion;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "resuelto_en")
    private Instant resueltoEn;

    protected Senalamiento() {
        // Requerido por JPA.
    }

    public Senalamiento(long reporteId, long ciudadanoId, long senaladoPor, String motivo) {
        this.reporteId = reporteId;
        this.ciudadanoId = ciudadanoId;
        this.senaladoPor = senaladoPor;
        this.motivo = motivo;
        this.creadoEn = Instant.now();
    }

    /** El administrador no encontró abuso. */
    public void desestimar(long administradorId, String motivoResolucion) {
        resolver(EstadoSenalamiento.desestimado, administradorId, motivoResolucion);
    }

    /** El administrador confirmó el abuso y suspendió la cuenta. */
    public void aceptar(long administradorId, String motivoResolucion) {
        resolver(EstadoSenalamiento.aceptado, administradorId, motivoResolucion);
    }

    private void resolver(EstadoSenalamiento nuevo, long administradorId, String motivoResolucion) {
        if (estado != EstadoSenalamiento.pendiente) {
            throw ReglaNegocioException.conflicto("SENALAMIENTO_RESUELTO", "Este señalamiento ya fue resuelto.");
        }
        this.estado = nuevo;
        this.resueltoPor = administradorId;
        this.resolucion = motivoResolucion;
        this.resueltoEn = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getReporteId() {
        return reporteId;
    }

    public Long getCiudadanoId() {
        return ciudadanoId;
    }

    public Long getSenaladoPor() {
        return senaladoPor;
    }

    public String getMotivo() {
        return motivo;
    }

    public EstadoSenalamiento getEstado() {
        return estado;
    }

    public String getResolucion() {
        return resolucion;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
