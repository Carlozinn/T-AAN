package mx.uv.taan.cuentas.infraestructura;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import mx.uv.taan.comun.error.LimiteExcedidoException;

/**
 * Limita por origen los registros, reenvíos de verificación y recuperaciones de contraseña
 * (tabla solicitud_cuenta, RNF-022), que envían correos y crean cuentas sin sesión.
 *
 * <p>Como en RegistroIntentosAcceso, cada solicitud se guarda en su propia transacción: cuenta
 * aunque la operación termine en error (correo repetido, CAPTCHA inválido). Las solicitudes
 * rechazadas por el límite no se guardan, para que este se levante al terminar la ventana.</p>
 */
@Component
public class LimiteSolicitudesCuenta {

    public enum Tipo { registro, reenvio_verificacion, recuperacion }

    private static final long VENTANA_SEGUNDOS = 3600;

    private final JdbcTemplate jdbc;
    private final int maxPorHora;

    public LimiteSolicitudesCuenta(JdbcTemplate jdbc, @Value("${taan.limites.solicitudes-por-hora:5}") int maxPorHora) {
        this.jdbc = jdbc;
        this.maxPorHora = maxPorHora;
    }

    /** Registra la solicitud o lanza 429 LIMITE_EXCEDIDO si el origen ya alcanzó el límite en la última hora. */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = LimiteExcedidoException.class)
    public void registrar(String origenIp, Tipo tipo) {
        Integer recientes = jdbc.queryForObject("""
                SELECT count(*) FROM solicitud_cuenta
                WHERE origen_ip = ?::inet AND tipo = ? AND creado_en > now() - interval '1 hour'""",
                Integer.class, origenIp, tipo.name());
        if (recientes != null && recientes >= maxPorHora) {
            throw new LimiteExcedidoException(VENTANA_SEGUNDOS);
        }
        jdbc.update("INSERT INTO solicitud_cuenta (origen_ip, tipo) VALUES (?::inet, ?)", origenIp, tipo.name());
    }
}
