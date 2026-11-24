package mx.uv.taan.notificaciones;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * Activa los avisos push si se definió taan.firebase.credenciales (TAAN_FIREBASE_CREDENCIALES: ruta
 * al JSON de la cuenta de servicio). Sin ella, el envío push se omite y el correo sigue funcionando.
 */
@Configuration
public class ConfiguracionNotificaciones {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracionNotificaciones.class);

    @Bean
    public EnviadorPush enviadorPush(@Value("${taan.firebase.credenciales:}") String rutaCredenciales)
            throws IOException {
        if (rutaCredenciales.isBlank()) {
            log.info("Avisos push deshabilitados: falta TAAN_FIREBASE_CREDENCIALES.");
            return (tokens, titulo, cuerpo, datos) -> List.of();
        }
        try (InputStream credenciales = new FileInputStream(rutaCredenciales)) {
            FirebaseOptions opciones = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credenciales))
                    .build();
            return new PushFirebase(FirebaseMessaging.getInstance(FirebaseApp.initializeApp(opciones, "taan")));
        }
    }
}
