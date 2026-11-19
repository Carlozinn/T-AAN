package mx.uv.taan.reportes.infraestructura;

import java.time.Year;
import java.time.ZoneId;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Genera el folio del reporte (RF-011): TAAN-<año>-<número>, p. ej. TAAN-2026-000123. El número
 * viene de la secuencia folio_reporte_seq (migración V2), que nunca entrega el mismo valor dos
 * veces aunque lleguen reportes al mismo tiempo. Cumple el patrón del contrato ^[A-Z0-9-]{6,20}$.
 */
@Component
public class SecuenciaFolios {

    private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

    private final JdbcTemplate jdbc;

    public SecuenciaFolios(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public String siguiente() {
        Long numero = jdbc.queryForObject("SELECT nextval('folio_reporte_seq')", Long.class);
        return "TAAN-%d-%06d".formatted(Year.now(ZONA).getValue(), numero);
    }
}
