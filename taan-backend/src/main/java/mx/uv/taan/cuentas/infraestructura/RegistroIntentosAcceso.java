package mx.uv.taan.cuentas.infraestructura;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import mx.uv.taan.comun.error.LimiteExcedidoException;

/**
 * Limita los intentos de inicio de sesión por cuenta y por origen (tabla intento_acceso,
 * RNF-009). No almacena contraseñas ni revela si un correo existe.
 *
 * <p>Nota: origen_ip guarda request.getRemoteAddr(); si el backend queda detrás de un
 * proxy inverso en el despliegue, habrá que tomar X-Forwarded-For con ForwardedHeaderFilter.</p>
 */
@Component
public class RegistroIntentosAcceso {

    private final JdbcTemplate jdbc;
    private final int maxFallidos;
    private final int ventanaMinutos;

    public RegistroIntentosAcceso(JdbcTemplate jdbc,
                                  @Value("${taan.limites.intentos-fallidos:5}") int maxFallidos,
                                  @Value("${taan.limites.ventana-minutos:15}") int ventanaMinutos) {
        this.jdbc = jdbc;
        this.maxFallidos = maxFallidos;
        this.ventanaMinutos = ventanaMinutos;
    }

    /**
     * Lanza 429 LIMITE_EXCEDIDO (y registra el intento como «limitado») si la cuenta o el
     * origen acumulan demasiados intentos fallidos en la ventana. Se llama antes de
     * comparar la contraseña, para que adivinar tampoco cueste cómputo.
     */
    public void asegurarDentroDelLimite(String origenIp, Long ciudadanoId, Long usuarioId) {
        boolean excedido = contarFallidos("origen_ip = ?::inet", origenIp) >= maxFallidos
                || (ciudadanoId != null && contarFallidos("ciudadano_id = ?", ciudadanoId) >= maxFallidos)
                || (usuarioId != null && contarFallidos("usuario_id = ?", usuarioId) >= maxFallidos);
        if (excedido) {
            registrar(origenIp, ciudadanoId, usuarioId, "limitado");
            throw new LimiteExcedidoException((long) ventanaMinutos * 60);
        }
    }

    public void registrarExitoso(String origenIp, Long ciudadanoId, Long usuarioId) {
        registrar(origenIp, ciudadanoId, usuarioId, "exitoso");
    }

    public void registrarFallido(String origenIp, Long ciudadanoId, Long usuarioId) {
        registrar(origenIp, ciudadanoId, usuarioId, "fallido");
    }

    private int contarFallidos(String condicion, Object valor) {
        Integer total = jdbc.queryForObject("""
                SELECT count(*) FROM intento_acceso
                WHERE resultado = 'fallido'
                  AND creado_en > now() - make_interval(mins => ?)
                  AND %s""".formatted(condicion), Integer.class, ventanaMinutos, valor);
        return total == null ? 0 : total;
    }

    private void registrar(String origenIp, Long ciudadanoId, Long usuarioId, String resultado) {
        jdbc.update("""
                INSERT INTO intento_acceso (origen_ip, ciudadano_id, usuario_id, resultado)
                VALUES (?::inet, ?, ?, ?::resultado_acceso)""",
                origenIp, ciudadanoId, usuarioId, resultado);
    }
}
