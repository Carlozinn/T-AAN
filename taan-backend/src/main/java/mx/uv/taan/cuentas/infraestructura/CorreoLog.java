package mx.uv.taan.cuentas.infraestructura;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mx.uv.taan.cuentas.aplicacion.EnviadorCorreo;

/**
 * Adaptador de desarrollo: en lugar de enviar el correo, deja el enlace en el log para
 * probar el flujo completo sin un servidor SMTP. Se usa cuando spring.mail.host no está
 * configurado (ConfiguracionAdaptadores).
 */
public class CorreoLog implements EnviadorCorreo {

    private static final Logger log = LoggerFactory.getLogger(CorreoLog.class);

    @Override
    public void enviarVerificacion(String correo, String nombre, String enlace) {
        log.info("[solo desarrollo] Enlace de verificación para {}: {}", correo, enlace);
    }
}
