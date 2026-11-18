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
}
