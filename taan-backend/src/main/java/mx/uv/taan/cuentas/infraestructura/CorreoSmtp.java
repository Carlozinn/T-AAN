package mx.uv.taan.cuentas.infraestructura;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import mx.uv.taan.cuentas.aplicacion.EnviadorCorreo;

/**
 * Adaptador real de correo (spring-boot-starter-mail). Textos breves y sin datos
 * sensibles; el enlace caduca solo (token firmado con vigencia).
 */
public class CorreoSmtp implements EnviadorCorreo {

    private final JavaMailSender sender;
    private final String remitente;

    public CorreoSmtp(JavaMailSender sender, String remitente) {
        this.sender = sender;
        this.remitente = remitente;
    }

    @Override
    public void enviarVerificacion(String correo, String nombre, String enlace) {
        enviar(correo, "Confirma tu cuenta de T'AAN", """
                Hola %s:

                Para activar tu cuenta de T'AAN entra a este enlace:

                %s

                El enlace caduca en 24 horas. Si tú no creaste la cuenta, ignora este correo.
                """.formatted(nombre, enlace));
    }

    @Override
    public void enviarRecuperacion(String correo, String nombre, String enlace) {
        enviar(correo, "Restablece tu contraseña de T'AAN", """
                Hola %s:

                Para elegir una nueva contraseña entra a este enlace:

                %s

                El enlace caduca en una hora y solo funciona una vez. Si tú no lo pediste,
                ignora este correo: tu contraseña actual sigue funcionando.
                """.formatted(nombre, enlace));
    }

    @Override
    public void enviarAvisoSuspension(String correo, String nombre, String motivo, String vigencia) {
        enviar(correo, "Tu cuenta de T'AAN fue suspendida", """
                Hola %s:

                Tu cuenta de T'AAN fue suspendida %s por el siguiente motivo:

                %s

                Mientras tanto puedes consultar tus reportes, pero no crear reportes nuevos ni reabrirlos.
                Tus reportes anteriores no se modifican.
                """.formatted(nombre, vigencia, motivo));
    }

    @Override
    public void enviarAvisoLevantamiento(String correo, String nombre, String motivo) {
        enviar(correo, "Tu cuenta de T'AAN está activa de nuevo", """
                Hola %s:

                Levantamos la suspensión de tu cuenta de T'AAN por el siguiente motivo:

                %s

                Desde ahora puedes volver a crear reportes y reabrirlos.
                """.formatted(nombre, motivo));
    }

    private void enviar(String correo, String asunto, String texto) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(correo);
        mensaje.setSubject(asunto);
        mensaje.setText(texto);
        sender.send(mensaje);
    }
}
