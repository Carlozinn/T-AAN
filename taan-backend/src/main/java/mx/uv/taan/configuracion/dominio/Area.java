package mx.uv.taan.configuracion.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Área interna de una organización (tabla area; RF-033, CU-13). La organización se guarda por
 * id porque su entidad pertenece al módulo de administración. creado_en lo pone la base.
 */
@Entity
@Table(name = "area")
public class Area {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organizacion_id", nullable = false, updatable = false)
    private Long organizacionId;

    @Column(nullable = false)
    private String nombre;

    /** Contacto oficial que ve el ciudadano si su reporte es rechazado (RF-036). */
    @Column(name = "telefono_publico")
    private String telefonoPublico;

    @Column(name = "correo_publico")
    private String correoPublico;

    /** Qué atiende el área; la usa la propuesta de correspondencia con IA (CU-22 paso 3). */
    private String funciones;

    @Column(nullable = false)
    private boolean activa = true;

    protected Area() {
        // Requerido por JPA.
    }

    public Area(Long organizacionId, String nombre, String telefonoPublico, String correoPublico) {
        this.organizacionId = organizacionId;
        this.nombre = nombre;
        this.telefonoPublico = telefonoPublico;
        this.correoPublico = correoPublico;
    }

    public void cambiarNombre(String nombre) {
        this.nombre = nombre;
    }

    public void cambiarContacto(String telefonoPublico, String correoPublico) {
        this.telefonoPublico = telefonoPublico;
        this.correoPublico = correoPublico;
    }

    public void cambiarFunciones(String funciones) {
        this.funciones = funciones;
    }

    /** Desactivar conserva el historial de los reportes que atendió (RF-033). */
    public void cambiarActiva(boolean activa) {
        this.activa = activa;
    }

    public Long getId() {
        return id;
    }

    public Long getOrganizacionId() {
        return organizacionId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTelefonoPublico() {
        return telefonoPublico;
    }

    public String getCorreoPublico() {
        return correoPublico;
    }

    public String getFunciones() {
        return funciones;
    }

    public boolean isActiva() {
        return activa;
    }
}
