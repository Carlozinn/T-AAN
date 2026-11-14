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
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(correo);
        mensaje.setSubject("Confirma tu cuenta de T'AAN");
        mensaje.setText("""
                Hola %s:

                Para activar tu cuenta de T'AAN entra a este enlace:

                %s

                El enlace caduca en 24 horas. Si tú no creaste la cuenta, ignora este correo.
                """.formatted(nombre, enlace));
        sender.send(mensaje);
    }
}
