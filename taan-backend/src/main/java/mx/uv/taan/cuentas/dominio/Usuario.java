package mx.uv.taan.cuentas.dominio;

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

/**
 * Usuario institucional (tabla usuario; RF-003, RF-032): personal de área, coordinación y
 * administradores de T'AAN. Las cuentas ciudadanas viven en {@link Ciudadano}.
 *
 * <p>La organización y el área se guardan por id (sin asociación JPA) porque este módulo solo
 * los necesita como referencia; sus entidades pertenecen al módulo de configuración.</p>
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String correo;

    /** Siempre BCrypt, nunca texto plano (RNF-006). */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    // El tipo de PostgreSQL se llama rol_usuario (no "rol"): columnDefinition se lo indica a Hibernate.
    @Column(nullable = false, columnDefinition = "rol_usuario")
    private Rol rol;

    @Column(name = "organizacion_id")
    private Long organizacionId;

    @Column(name = "area_id")
    private Long areaId;

    /** true obliga a cambiar la contraseña en el siguiente acceso (RF-003). */
    @Column(name = "credencial_temporal", nullable = false)
    private boolean credencialTemporal;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "ultimo_acceso")
    private Instant ultimoAcceso;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    protected Usuario() {
        // Requerido por JPA.
    }

    private Usuario(String nombre, String correo, String passwordHash, Rol rol,
                    Long organizacionId, Long areaId) {
        this.nombre = nombre;
        this.correo = correo;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.organizacionId = organizacionId;
        this.areaId = areaId;
        this.credencialTemporal = true;
        this.creadoEn = Instant.now();
    }

    /**
     * Cuenta nueva de personal con credencial temporal (CU-12): el CHECK de la tabla exige
     * administrador sin organización, coordinación con organización y personal de área con ambas.
     */
    public static Usuario conCredencialTemporal(String nombre, String correo, String passwordHash,
                                                Rol rol, Long organizacionId, Long areaId) {
        if (!rol.esInstitucional()) {
            throw new IllegalArgumentException("Los ciudadanos no viven en la tabla usuario.");
        }
        return new Usuario(nombre, correo, passwordHash, rol, organizacionId, areaId);
    }

    /** Cambia la contraseña y apaga la credencial temporal (CU-04). */
    public void cambiarPassword(String nuevoHash) {
        this.passwordHash = nuevoHash;
        this.credencialTemporal = false;
    }

    /** Asigna una nueva credencial temporal (CU-12: restablecimiento por la coordinación). */
    public void restablecerCredencial(String hashTemporal) {
        this.passwordHash = hashTemporal;
        this.credencialTemporal = true;
    }

    public void cambiarNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * Cambia rol y área juntos para respetar el CHECK de la tabla: coordinación sin área,
     * personal de área con área. Quien llama valida que el área sea de la misma organización.
     */
    public void cambiarRol(Rol nuevoRol, Long nuevaAreaId) {
        if (nuevoRol != Rol.coordinacion && nuevoRol != Rol.personal_area) {
            throw new IllegalArgumentException("Solo se cambia entre coordinación y personal de área.");
        }
        this.rol = nuevoRol;
        this.areaId = nuevoRol == Rol.coordinacion ? null : nuevaAreaId;
    }

    /** Desactivar no borra la cuenta ni su historial; solo impide iniciar sesión (RF-032). */
    public void cambiarActivo(boolean activo) {
        this.activo = activo;
    }

    public void registrarAcceso() {
        this.ultimoAcceso = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getCorreo() {
        return correo;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getNombre() {
        return nombre;
    }

    public Rol getRol() {
        return rol;
    }

    public Long getOrganizacionId() {
        return organizacionId;
    }

    public Long getAreaId() {
        return areaId;
    }

    public boolean isCredencialTemporal() {
        return credencialTemporal;
    }

    public boolean isActivo() {
        return activo;
    }

    public Instant getUltimoAcceso() {
        return ultimoAcceso;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
