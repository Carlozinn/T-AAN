package mx.uv.taan.cuentas.infraestructura;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.RestClient;

import mx.uv.taan.cuentas.aplicacion.EnviadorCorreo;
import mx.uv.taan.cuentas.aplicacion.VerificadorCaptcha;
import mx.uv.taan.cuentas.aplicacion.VerificadorGoogle;

/**
 * Elige la implementación de cada puerto del módulo cuentas según la configuración:
 * con spring.mail.host el correo sale por SMTP, sin él se registra en el log; el CAPTCHA
 * y Google se activan al definir sus secretos en application.yml.
 */
@Configuration
public class ConfiguracionAdaptadores {

    /**
     * Spring solo crea el JavaMailSender cuando existe spring.mail.host; si no hay, se usa
     * el adaptador de desarrollo que deja el enlace en el log.
     */
    @Bean
    public EnviadorCorreo enviadorCorreo(ObjectProvider<JavaMailSender> sender,
                                         @Value("${spring.mail.username:no-responder@taan.mx}") String remitente) {
        JavaMailSender disponible = sender.getIfAvailable();
        return disponible == null ? new CorreoLog() : new CorreoSmtp(disponible, remitente);
    }

    /**
     * RestClient.create() no necesita el starter spring-boot-starter-restclient (en Spring Boot 4
     * el RestClient.Builder inyectable ya no viene con el starter web).
     */
    @Bean
    public VerificadorCaptcha verificadorCaptcha(@Value("${taan.captcha.secreto:}") String secreto) {
        return new CaptchaTurnstile(RestClient.create(), secreto);
    }

    @Bean
    public VerificadorGoogle verificadorGoogle(@Value("${taan.google.client-id:}") String clientId) {
        return new VerificadorGoogleJwks(clientId);
    }
}
