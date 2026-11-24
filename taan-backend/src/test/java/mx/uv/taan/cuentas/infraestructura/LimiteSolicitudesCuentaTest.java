package mx.uv.taan.cuentas.infraestructura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import mx.uv.taan.comun.error.LimiteExcedidoException;

class LimiteSolicitudesCuentaTest {

    private static final String IP = "203.0.113.5";

    private JdbcTemplate jdbc;
    private LimiteSolicitudesCuenta limite;

    @BeforeEach
    void preparar() {
        jdbc = mock(JdbcTemplate.class);
        limite = new LimiteSolicitudesCuenta(jdbc, 5);
    }

    @Test
    void dentroDelLimiteRegistraLaSolicitudConSuTipo() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(IP), eq("recuperacion"))).thenReturn(4);

        limite.registrar(IP, LimiteSolicitudesCuenta.Tipo.recuperacion);

        verify(jdbc).update(contains("INSERT INTO solicitud_cuenta"), eq(IP), eq("recuperacion"));
    }

    @Test
    void alAlcanzarElLimiteResponde429SinGuardarLaSolicitud() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(IP), eq("registro"))).thenReturn(5);

        LimiteExcedidoException error = assertThrows(LimiteExcedidoException.class,
                () -> limite.registrar(IP, LimiteSolicitudesCuenta.Tipo.registro));

        assertEquals(429, error.getEstadoHttp());
        assertEquals(3600, error.getReintentarEnSegundos());
        verify(jdbc, never()).update(anyString(), eq(IP), eq("registro"));
    }
}
