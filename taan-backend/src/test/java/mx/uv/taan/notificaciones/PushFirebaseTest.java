package mx.uv.taan.notificaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;

/** FirebaseMessaging simulado: nunca se llama al servicio real. */
class PushFirebaseTest {

    private FirebaseMessaging mensajeria;
    private PushFirebase push;

    @BeforeEach
    void preparar() {
        mensajeria = mock(FirebaseMessaging.class);
        push = new PushFirebase(mensajeria);
    }

    @Test
    void devuelveSoloLosTokensQueFirebaseReportaComoVencidosOInvalidos() throws Exception {
        List<SendResponse> resultados = List.of(exito(), fallo(MessagingErrorCode.UNREGISTERED),
                fallo(MessagingErrorCode.UNAVAILABLE), fallo(MessagingErrorCode.INVALID_ARGUMENT));
        BatchResponse respuesta = mock(BatchResponse.class);
        when(respuesta.getResponses()).thenReturn(resultados);
        when(mensajeria.sendEachForMulticast(any())).thenReturn(respuesta);

        List<String> vencidos = push.enviar(List.of("ok", "desinstalado", "sin-servicio", "malformado"),
                "T'AAN", "Tu reporte cambió.", Map.of("folio", "TAAN-2026-000001"));

        assertEquals(List.of("desinstalado", "malformado"), vencidos);
    }

    @Test
    void unErrorGeneralDeFirebaseNoBorraTokensNiSePropaga() throws Exception {
        FirebaseMessagingException error = mock(FirebaseMessagingException.class);
        when(mensajeria.sendEachForMulticast(any())).thenThrow(error);

        assertEquals(List.of(), push.enviar(List.of("token"), "T'AAN", "Tu reporte cambió.", Map.of()));
    }

    private static SendResponse exito() {
        SendResponse respuesta = mock(SendResponse.class);
        when(respuesta.isSuccessful()).thenReturn(true);
        return respuesta;
    }

    private static SendResponse fallo(MessagingErrorCode codigo) {
        FirebaseMessagingException error = mock(FirebaseMessagingException.class);
        when(error.getMessagingErrorCode()).thenReturn(codigo);
        SendResponse respuesta = mock(SendResponse.class);
        when(respuesta.isSuccessful()).thenReturn(false);
        when(respuesta.getException()).thenReturn(error);
        return respuesta;
    }
}
