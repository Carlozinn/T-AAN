package mx.uv.taan.notificaciones;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import mx.uv.taan.cuentas.aplicacion.EnviadorCorreo;
import mx.uv.taan.cuentas.aplicacion.ServicioDispositivos;
import mx.uv.taan.cuentas.aplicacion.ServicioPerfilCiudadano;
import mx.uv.taan.reportes.aplicacion.AvisoCiudadano;

/**
 * Envía los avisos de seguimiento (CU-02; RF-034, RF-044): push a cada dispositivo de la cuenta y,
 * si la cuenta lo activó, un correo con el enlace a «Mis reportes». Escucha {@link AvisoCiudadano}
 * solo cuando el cambio del reporte ya se confirmó, y en segundo plano: un fallo de Firebase o del
 * correo queda en el log y nunca deshace ni retrasa el cambio.
 */
@Service
public class NotificadorCiudadano implements Notificador {

    private static final Logger log = LoggerFactory.getLogger(NotificadorCiudadano.class);
    private static final String TITULO = "T'AAN";

    private final ServicioDispositivos dispositivos;
    private final ServicioPerfilCiudadano perfiles;
    private final EnviadorPush push;
    private final EnviadorCorreo correo;
    private final String urlWeb;

    public NotificadorCiudadano(ServicioDispositivos dispositivos, ServicioPerfilCiudadano perfiles,
                                EnviadorPush push, EnviadorCorreo correo,
                                @Value("${taan.web.url:http://localhost:4200}") String urlWeb) {
        this.dispositivos = dispositivos;
        this.perfiles = perfiles;
        this.push = push;
        this.correo = correo;
        this.urlWeb = urlWeb;
    }

    @Async
    @TransactionalEventListener
    public void alConfirmarse(AvisoCiudadano aviso) {
        avisar(aviso.ciudadanoId(), aviso.folio(), aviso.resumen());
    }

    @Override
    public void avisar(long ciudadanoId, String folio, String resumen) {
        try {
            List<String> tokens = dispositivos.tokensDe(ciudadanoId);
            if (!tokens.isEmpty()) {
                dispositivos.eliminar(ciudadanoId, push.enviar(tokens, TITULO, resumen, Map.of("folio", folio)));
            }
        } catch (RuntimeException e) {
            log.warn("Falló el aviso push del reporte {}: {}", folio, e.getMessage());
        }
        try {
            perfiles.destinatarioAvisosCorreo(ciudadanoId).ifPresent(d ->
                    correo.enviarAvisoReporte(d.correo(), d.nombre(), folio, resumen, urlWeb + "/mis-reportes/" + folio));
        } catch (RuntimeException e) {
            log.warn("Falló el aviso por correo del reporte {}: {}", folio, e.getMessage());
        }
    }
}
