package mx.uv.taan.administracion.dominio;

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

/**
 * Ayuntamiento u otra institución (tabla organizacion; RF-027, CU-11). Las fechas de creación
 * y actualización las ponen la base y su disparador.
 */
@Entity
@Table(name = "organizacion")
public class Organizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "nivel_participacion", nullable = false, columnDefinition = "nivel_participacion")
    private NivelParticipacion nivelParticipacion;

    /** Días para que el ciudadano reabra un reporte resuelto (RF-042). */
    @Column(name = "plazo_reapertura_dias", nullable = false)
    private int plazoReaperturaDias;

    @Column(nullable = false)
    private boolean activa = true;

    protected Organizacion() {
        // Requerido por JPA.
    }

    public Organizacion(String nombre, NivelParticipacion nivelParticipacion, int plazoReaperturaDias) {
        this.nombre = nombre;
        this.nivelParticipacion = nivelParticipacion;
        this.plazoReaperturaDias = plazoReaperturaDias;
    }

    public void cambiarNombre(String nombre) {
        this.nombre = nombre;
    }

    public void cambiarNivelParticipacion(NivelParticipacion nivel) {
        this.nivelParticipacion = nivel;
    }

    public void cambiarPlazoReapertura(int dias) {
        this.plazoReaperturaDias = dias;
    }

    /** Desactivar no elimina el historial (RF-027). */
    public void cambiarActiva(boolean activa) {
        this.activa = activa;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public NivelParticipacion getNivelParticipacion() {
        return nivelParticipacion;
    }

    public int getPlazoReaperturaDias() {
        return plazoReaperturaDias;
    }

    public boolean isActiva() {
        return activa;
    }
}
