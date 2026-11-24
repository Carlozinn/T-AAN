package mx.uv.taan.cuentas.aplicacion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import mx.uv.taan.cuentas.infraestructura.SuscripcionAvisoRepository;

class ServicioDispositivosTest {

    private SuscripcionAvisoRepository suscripciones;
    private ServicioDispositivos servicio;

    @BeforeEach
    void preparar() {
        suscripciones = mock(SuscripcionAvisoRepository.class);
        servicio = new ServicioDispositivos(suscripciones);
    }

    @Test
    void registrarQuitaElDispositivoDeOtrasCuentasYLoAsociaALaActual() {
        servicio.registrar(4L, "  token-fcm  ");

        InOrder orden = inOrder(suscripciones);
        orden.verify(suscripciones).eliminarDeOtrasCuentas(4L, "token-fcm");
        orden.verify(suscripciones).registrar(4L, "token-fcm");
    }

    @Test
    void darDeBajaEliminaSoloEseDispositivoDeLaCuenta() {
        servicio.darDeBaja(4L, "token-fcm ");

        verify(suscripciones).eliminar(4L, Set.of("token-fcm"));
    }

    @Test
    void sinTokensVencidosNoConsultaLaBase() {
        servicio.eliminar(4L, List.of());

        verify(suscripciones, never()).eliminar(anyLong(), any());
    }
}
