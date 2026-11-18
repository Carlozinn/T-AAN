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
 * Canal oficial de una organización externa que ve el ciudadano cuando su reporte se canaliza
 * fuera de T'AAN (tabla canal_externo; RF-036, CU-20). Desactivar conserva el historial.
 */
@Entity
@Table(name = "canal_externo")
public class CanalExterno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organizacion_id", nullable = false, updatable = false)
    private Long organizacionId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "tipo_canal")
    private TipoCanal tipo;

    /** Número, URL o correo oficial. */
    @Column(nullable = false)
    private String valor;

    /** Dónde se verificó que el canal es oficial (se le muestra al ciudadano solo si es confiable). */
    @Column(name = "fuente_verificacion", nullable = false)
    private String fuenteVerificacion;

    @Column(nullable = false)
    private boolean activo = true;

    protected CanalExterno() {
        // Requerido por JPA.
    }

    public CanalExterno(Long organizacionId, TipoCanal tipo, String valor, String fuenteVerificacion) {
        this.organizacionId = organizacionId;
        this.tipo = tipo;
        this.valor = valor;
        this.fuenteVerificacion = fuenteVerificacion;
    }

    public void cambiarCanal(TipoCanal tipo, String valor) {
        this.tipo = tipo;
        this.valor = valor;
    }

    public void cambiarFuente(String fuenteVerificacion) {
        this.fuenteVerificacion = fuenteVerificacion;
    }

    public void cambiarActivo(boolean activo) {
        this.activo = activo;
    }

    public Long getId() {
        return id;
    }

    public Long getOrganizacionId() {
        return organizacionId;
    }

    public TipoCanal getTipo() {
        return tipo;
    }

    public String getValor() {
        return valor;
    }

    public String getFuenteVerificacion() {
        return fuenteVerificacion;
    }

    public boolean isActivo() {
        return activo;
    }
}
