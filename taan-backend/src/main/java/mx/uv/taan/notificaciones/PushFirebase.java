package mx.uv.taan.notificaciones;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;

/**
 * Avisos push con Firebase Cloud Messaging. Un token con UNREGISTERED o INVALID_ARGUMENT ya no
 * sirve (la app se desinstaló o el token se renovó) y se devuelve para eliminarlo; cualquier otro
 * error (red, cuota) solo se registra y el token se conserva.
 */
class PushFirebase implements EnviadorPush {

    private static final Logger log = LoggerFactory.getLogger(PushFirebase.class);

    private final FirebaseMessaging mensajeria;

    PushFirebase(FirebaseMessaging mensajeria) {
        this.mensajeria = mensajeria;
    }

    @Override
    public List<String> enviar(List<String> tokens, String titulo, String cuerpo, Map<String, String> datos) {
        MulticastMessage mensaje = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder().setTitle(titulo).setBody(cuerpo).build())
                .putAllData(datos)
                .build();
        BatchResponse respuesta;
        try {
            respuesta = mensajeria.sendEachForMulticast(mensaje);
        } catch (FirebaseMessagingException e) {
            log.warn("No se pudo enviar el aviso push: {}", e.getMessage());
            return List.of();
        }
        List<String> vencidos = new ArrayList<>();
        List<SendResponse> resultados = respuesta.getResponses();
        for (int i = 0; i < resultados.size(); i++) {
            SendResponse resultado = resultados.get(i);
            if (resultado.isSuccessful()) {
                continue;
            }
            MessagingErrorCode codigo = resultado.getException().getMessagingErrorCode();
            if (codigo == MessagingErrorCode.UNREGISTERED || codigo == MessagingErrorCode.INVALID_ARGUMENT) {
                vencidos.add(tokens.get(i));
            } else {
                log.warn("Aviso push no entregado ({}): {}", codigo, resultado.getException().getMessage());
            }
        }
        return vencidos;
    }
}
