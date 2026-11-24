package mx.uv.taan.cuentas.aplicacion;

/**
 * Puerto de salida para los correos de cuenta (CU-24). Los servicios dependen de esta
 * interfaz y no de SMTP: en desarrollo el enlace sale por el log (CorreoLog) y con
 * spring.mail configurado se envía de verdad (CorreoSmtp).
 */
public interface EnviadorCorreo {

    /** Envía el enlace para confirmar la cuenta nueva (RF-001). */
    void enviarVerificacion(String correo, String nombre, String enlace);

    /** Envía el enlace para restablecer la contraseña olvidada (CU-04). */
    void enviarRecuperacion(String correo, String nombre, String enlace);

    /** Avisa la suspensión de la cuenta con su motivo y vigencia (RF-043; vigencia "indefinida" si no tiene fecha). */
    void enviarAvisoSuspension(String correo, String nombre, String motivo, String vigencia);

    /** Avisa que el administrador levantó la suspensión, con su motivo (RF-043). */
    void enviarAvisoLevantamiento(String correo, String nombre, String motivo);

    /** Avisa una novedad de un reporte (RF-044): solo el folio y el resumen, con el enlace a «Mis reportes». */
    void enviarAvisoReporte(String correo, String nombre, String folio, String resumen, String enlace);
}
