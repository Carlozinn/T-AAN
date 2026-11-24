package mx.uv.taan.cuentas.dominio;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Auditoría de una suspensión levantada por el administrador (tabla levantamiento_suspension; RF-043). Solo se inserta. */
@Entity
@Table(name = "levantamiento_suspension")
public class LevantamientoSuspension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nulo si la cuenta se eliminó después. */
    @Column(name = "ciudadano_id")
    private Long ciudadanoId;

    @Column(name = "levantado_por", nullable = false, updatable = false)
    private Long levantadoPor;

    @Column(nullable = false, updatable = false)
    private String motivo;

    @Column(name = "motivo_suspension_anterior", nullable = false, updatable = false)
    private String motivoSuspensionAnterior;

    /** Nulo = la suspensión interrumpida era indefinida. */
    @Column(name = "suspendida_hasta_anterior", updatable = false)
    private Instant suspendidaHastaAnterior;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    protected LevantamientoSuspension() {
        // Requerido por JPA.
    }

    /** Toma de la cuenta la suspensión que se interrumpe; se crea antes de levantarla. */
    public LevantamientoSuspension(Ciudadano cuenta, long levantadoPor, String motivo) {
        this.ciudadanoId = cuenta.getId();
        this.levantadoPor = levantadoPor;
        this.motivo = motivo;
        this.motivoSuspensionAnterior = cuenta.getMotivoSuspension();
        this.suspendidaHastaAnterior = cuenta.getSuspendidaHasta();
        this.creadoEn = Instant.now();
    }

    public Long getCiudadanoId() {
        return ciudadanoId;
    }

    public Long getLevantadoPor() {
        return levantadoPor;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getMotivoSuspensionAnterior() {
        return motivoSuspensionAnterior;
    }

    public Instant getSuspendidaHastaAnterior() {
        return suspendidaHastaAnterior;
    }
}
