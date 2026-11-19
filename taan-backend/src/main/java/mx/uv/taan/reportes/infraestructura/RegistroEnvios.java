package mx.uv.taan.reportes.infraestructura;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import mx.uv.taan.comun.error.LimiteExcedidoException;

/**
 * Registro técnico antiabuso de cada envío de reporte (tabla registro_envio; RNF-022):
 * origen, cuenta, agente de usuario y resultado. No lo ve el personal.
 *
 * <p>Los rechazos se guardan en su propia transacción (REQUIRES_NEW), porque la petición
 * termina en error y su transacción se revierte. El envío aceptado, en cambio, se guarda en
 * la misma transacción que el reporte, con su reporte_id.</p>
 */
@Component
public class RegistroEnvios {

    private static final String MENSAJE_LIMITE = "Por seguridad, por ahora no podemos recibir más reportes desde "
            + "tu cuenta. Podrás enviarlo más tarde; si es una emergencia, llama al 911.";

    private final JdbcTemplate jdbc;
    private final int porHora;
    private final int porDia;

    public RegistroEnvios(JdbcTemplate jdbc,
                          @Value("${taan.limites.reportes-por-hora:5}") int porHora,
                          @Value("${taan.limites.reportes-por-dia:20}") int porDia) {
        this.jdbc = jdbc;
        this.porHora = porHora;
        this.porDia = porDia;
    }

    /**
     * Límite configurable por cuenta y por origen (sugerido: 5 por hora y 20 por día). Al
     * excederlo registra el rechazo y responde 429 con un mensaje preventivo (CU-01 EX-06).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = LimiteExcedidoException.class)
    public void asegurarDentroDelLimite(String origenIp, long ciudadanoId, String agente) {
        boolean excedeHora = aceptados("ciudadano_id = ?", ciudadanoId, 1) >= porHora
                || aceptados("origen_ip = ?::inet", origenIp, 1) >= porHora;
        boolean excedeDia = aceptados("ciudadano_id = ?", ciudadanoId, 24) >= porDia
                || aceptados("origen_ip = ?::inet", origenIp, 24) >= porDia;
        if (excedeHora || excedeDia) {
            insertar(origenIp, ciudadanoId, agente, "rechazado_limite", null);
            throw new LimiteExcedidoException(excedeDia ? 24 * 3600 : 3600, MENSAJE_LIMITE);
        }
    }

    /** rechazado_honeypot, rechazado_verificacion o rechazado_suspension. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarRechazo(String origenIp, long ciudadanoId, String agente, String resultado) {
        insertar(origenIp, ciudadanoId, agente, resultado, null);
    }

    /** Dentro de la transacción del reporte: si el reporte no se guarda, tampoco esto. */
    public void registrarAceptado(String origenIp, long ciudadanoId, String agente, long reporteId) {
        insertar(origenIp, ciudadanoId, agente, "aceptado", reporteId);
    }

    private int aceptados(String condicion, Object valor, int horas) {
        Integer total = jdbc.queryForObject("""
                SELECT count(*) FROM registro_envio
                WHERE resultado = 'aceptado' AND creado_en > now() - make_interval(hours => ?) AND %s"""
                .formatted(condicion), Integer.class, horas, valor);
        return total == null ? 0 : total;
    }

    private void insertar(String origenIp, long ciudadanoId, String agente, String resultado, Long reporteId) {
        String agenteRecortado = agente == null ? null : agente.substring(0, Math.min(agente.length(), 500));
        jdbc.update("""
                INSERT INTO registro_envio (origen_ip, ciudadano_id, agente_usuario, resultado, reporte_id)
                VALUES (?::inet, ?, ?, ?::resultado_envio, ?)""",
                origenIp, ciudadanoId, agenteRecortado, resultado, reporteId);
    }
}
