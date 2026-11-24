package mx.uv.taan.notificaciones;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import mx.uv.taan.cuentas.aplicacion.DestinatarioAviso;
import mx.uv.taan.cuentas.aplicacion.EnviadorCorreo;
import mx.uv.taan.cuentas.aplicacion.ServicioDispositivos;
import mx.uv.taan.cuentas.aplicacion.ServicioPerfilCiudadano;
import mx.uv.taan.reportes.aplicacion.AvisoCiudadano;

class NotificadorCiudadanoTest {

    private static final String FOLIO = "TAAN-2026-000001";
    private static final String RESUMEN = "Tu reporte TAAN-2026-000001 ahora está «Resuelto».";

    private ServicioDispositivos dispositivos;
    private ServicioPerfilCiudadano perfiles;
    private EnviadorPush push;
    private EnviadorCorreo correo;
    private NotificadorCiudadano notificador;

    @BeforeEach
    void preparar() {
        dispositivos = mock(ServicioDispositivos.class);
        perfiles = mock(ServicioPerfilCiudadano.class);
        push = mock(EnviadorPush.class);
        correo = mock(EnviadorCorreo.class);
        notificador = new NotificadorCiudadano(dispositivos, perfiles, push, correo, "http://localhost:4200");
        when(dispositivos.tokensDe(7L)).thenReturn(List.of("token-a", "token-b"));
        when(perfiles.destinatarioAvisosCorreo(7L)).thenReturn(Optional.of(new DestinatarioAviso("vecina@correo.mx", "Vecina")));
    }

    @Test
    void enviaElPushATodosLosDispositivosYElCorreoConElEnlaceAMisReportes() {
        when(push.enviar(anyList(), anyString(), anyString(), any())).thenReturn(List.of());

        notificador.alConfirmarse(new AvisoCiudadano(7L, FOLIO, RESUMEN));

        verify(push).enviar(List.of("token-a", "token-b"), "T'AAN", RESUMEN, Map.of("folio", FOLIO));
        verify(correo).enviarAvisoReporte("vecina@correo.mx", "Vecina", FOLIO, RESUMEN,
                "http://localhost:4200/mis-reportes/" + FOLIO);
    }

    @Test
    void losTokensVencidosSeDanDeBaja() {
        when(push.enviar(anyList(), anyString(), anyString(), any())).thenReturn(List.of("token-b"));

        notificador.avisar(7L, FOLIO, RESUMEN);

        verify(dispositivos).eliminar(7L, List.of("token-b"));
    }

    @Test
    void sinDispositivosNoSeLlamaAlServicioPush() {
        when(dispositivos.tokensDe(7L)).thenReturn(List.of());

        notificador.avisar(7L, FOLIO, RESUMEN);

        verify(push, never()).enviar(anyList(), anyString(), anyString(), any());
        verify(correo).enviarAvisoReporte(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void sinAvisosPorCorreoActivadosSoloSaleElPush() {
        when(perfiles.destinatarioAvisosCorreo(7L)).thenReturn(Optional.empty());

        notificador.avisar(7L, FOLIO, RESUMEN);

        verify(push).enviar(anyList(), anyString(), anyString(), any());
        verify(correo, never()).enviarAvisoReporte(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void unFalloDelPushNoImpideElCorreoNiSePropaga() {
        when(push.enviar(anyList(), anyString(), anyString(), any())).thenThrow(new IllegalStateException("sin red"));

        notificador.avisar(7L, FOLIO, RESUMEN);

        verify(correo).enviarAvisoReporte(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void unFalloDelCorreoNoSePropaga() {
        when(push.enviar(anyList(), anyString(), anyString(), any())).thenReturn(List.of());
        org.mockito.Mockito.doThrow(new IllegalStateException("SMTP caído"))
                .when(correo).enviarAvisoReporte(anyString(), anyString(), anyString(), anyString(), anyString());

        notificador.avisar(7L, FOLIO, RESUMEN);
    }
}
