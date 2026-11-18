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
 * Cuenta ciudadana (tabla ciudadano; RF-001, RF-012, RF-038, RF-043).
 *
 * <p>Se crea solo con los métodos de fábrica {@link #conCorreo} y {@link #conGoogle}, que
 * garantizan en Java los mismos invariantes que el CHECK de la tabla: una cuenta Google
 * tiene google_sub y no tiene contraseña; una cuenta de correo tiene contraseña cifrada.</p>
 */
@Entity
@Table(name = "ciudadano")
public class Ciudadano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String correo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "metodo_acceso", nullable = false, columnDefinition = "metodo_acceso")
    private MetodoAcceso metodoAcceso;

    @Column(name = "google_sub")
    private String googleSub;

    /** Siempre BCrypt, nunca texto plano (RNF-006). Nulo en cuentas Google. */
    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "correo_verificado", nullable = false)
    private boolean correoVerificado;

    /** Obligatorio antes del primer reporte (RF-038); lo completa el módulo de perfil. */
    @Column(name = "telefono_cifrado")
    private String telefonoCifrado;

    @Column(name = "avisos_correo", nullable = false)
    private boolean avisosCorreo = true;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "estado_cuenta")
    private EstadoCuenta estado = EstadoCuenta.activa;

    @Column(name = "motivo_suspension")
    private String motivoSuspension;

    /** Nulo con estado suspendida = suspensión indefinida (reincidencia, RF-043). */
    @Column(name = "suspendida_hasta")
    private Instant suspendidaHasta;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    /** La actualiza también el disparador tg_ciudadano_fecha en la base. */
    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    protected Ciudadano() {
        // Requerido por JPA.
    }

    private Ciudadano(String nombre, String correo, MetodoAcceso metodoAcceso,
                      String googleSub, String passwordHash, boolean correoVerificado) {
        Instant ahora = Instant.now();
        this.nombre = nombre;
        this.correo = correo;
        this.metodoAcceso = metodoAcceso;
        this.googleSub = googleSub;
        this.passwordHash = passwordHash;
        this.correoVerificado = correoVerificado;
        this.creadoEn = ahora;
        this.actualizadoEn = ahora;
    }

    /** Cuenta nueva con correo y contraseña (CU-24): nace sin verificar. */
    public static Ciudadano conCorreo(String nombre, String correo, String passwordHash) {
        return new Ciudadano(nombre, correo, MetodoAcceso.correo, null, passwordHash, false);
    }

    /** Cuenta nueva con Google (CU-24): Google ya verificó el correo. */
    public static Ciudadano conGoogle(String nombre, String correo, String googleSub) {
        return new Ciudadano(nombre, correo, MetodoAcceso.google, googleSub, null, true);
    }

    /** Confirma el enlace de verificación (idempotente: confirmar dos veces no estorba). */
    public void verificarCorreo() {
        this.correoVerificado = true;
    }

    /** Cambia la contraseña; quien llama valida antes la actual y que el método sea correo. */
    public void cambiarPasswordHash(String nuevoHash) {
        this.passwordHash = nuevoHash;
    }

    /**
     * Restablece la contraseña olvidada (CU-04). Usar el enlace prueba que el ciudadano
     * controla el correo, así que también queda verificado.
     */
    public void restablecerPassword(String nuevoHash) {
        this.passwordHash = nuevoHash;
        this.correoVerificado = true;
    }

    /** Corrige el nombre visible (CU-25). */
    public void cambiarNombre(String nombre) {
        this.nombre = nombre;
    }

    /** Guarda el teléfono ya cifrado (RF-038); el cifrado lo hace el servicio. */
    public void cambiarTelefonoCifrado(String telefonoCifrado) {
        this.telefonoCifrado = telefonoCifrado;
    }

    /** Preferencia de avisos por correo (RF-044). */
    public void cambiarAvisosCorreo(boolean avisosCorreo) {
        this.avisosCorreo = avisosCorreo;
    }

    public boolean esDeGoogle() {
        return metodoAcceso == MetodoAcceso.google;
    }

    /** true si falta el teléfono: el ciudadano debe completarlo antes de reportar (RF-038). */
    public boolean requiereTelefono() {
        return telefonoCifrado == null;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public MetodoAcceso getMetodoAcceso() {
        return metodoAcceso;
    }

    public String getGoogleSub() {
        return googleSub;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isCorreoVerificado() {
        return correoVerificado;
    }

    public String getTelefonoCifrado() {
        return telefonoCifrado;
    }

    public boolean isAvisosCorreo() {
        return avisosCorreo;
    }

    public EstadoCuenta getEstado() {
        return estado;
    }

    public String getMotivoSuspension() {
        return motivoSuspension;
    }

    public Instant getSuspendidaHasta() {
        return suspendidaHasta;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
