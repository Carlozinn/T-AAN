package mx.uv.taan.informes.dominio;

import java.time.Instant;
import java.time.LocalDate;

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

/** Quién generó cada informe y cuándo (tabla informe_generado; RF-026, punto 9). Solo se inserta. */
@Entity
@Table(name = "informe_generado")
public class InformeGenerado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Long usuarioId;

    @Column(name = "organizacion_id", nullable = false, updatable = false)
    private Long organizacionId;

    @Column(name = "area_id", updatable = false)
    private Long areaId;

    @Column(name = "periodo_inicio", nullable = false, updatable = false)
    private LocalDate periodoInicio;

    @Column(name = "periodo_fin", nullable = false, updatable = false)
    private LocalDate periodoFin;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, updatable = false, columnDefinition = "formato_informe")
    private FormatoInforme formato;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    protected InformeGenerado() {
        // Requerido por JPA.
    }

    public InformeGenerado(long usuarioId, long organizacionId, Long areaId, LocalDate periodoInicio,
                           LocalDate periodoFin, FormatoInforme formato) {
        this.usuarioId = usuarioId;
        this.organizacionId = organizacionId;
        this.areaId = areaId;
        this.periodoInicio = periodoInicio;
        this.periodoFin = periodoFin;
        this.formato = formato;
        this.creadoEn = Instant.now();
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Long getAreaId() {
        return areaId;
    }

    public FormatoInforme getFormato() {
        return formato;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
