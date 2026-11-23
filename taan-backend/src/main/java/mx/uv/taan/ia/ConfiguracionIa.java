package mx.uv.taan.ia;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.ObjectMapper;

/**
 * Arma los proveedores en el orden del Anexo E (Gemini y después Groq) con un tiempo límite por
 * llamada. Las claves vienen de variables de entorno (GEMINI_API_KEY, GROQ_API_KEY); sin clave el
 * proveedor se salta.
 */
@Configuration
public class ConfiguracionIa {

    @Bean
    CadenaProveedores cadenaProveedoresIa(@Value("${taan.ia.gemini.api-key}") String claveGemini,
                                          @Value("${taan.ia.gemini.modelo}") String modeloGemini,
                                          @Value("${taan.ia.groq.api-key}") String claveGroq,
                                          @Value("${taan.ia.groq.modelo}") String modeloGroq,
                                          @Value("${taan.ia.timeout-segundos}") int timeoutSegundos) {
        Duration limite = Duration.ofSeconds(timeoutSegundos);
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(limite).build());
        fabrica.setReadTimeout(limite);
        RestClient http = RestClient.builder().requestFactory(fabrica).build();
        return new CadenaProveedores(List.of(new ProveedorGemini(http, claveGemini, modeloGemini),
                new ProveedorGroq(http, claveGroq, modeloGroq)));
    }

    @Bean
    public ClasificadorIa clasificadorIa(CadenaProveedores cadenaProveedoresIa, ObjectMapper json) {
        return new ClasificadorConRespaldo(cadenaProveedoresIa, json);
    }

    @Bean
    public ProponedorCorrespondencia proponedorCorrespondencia(CadenaProveedores cadenaProveedoresIa,
                                                               ObjectMapper json) {
        return new ProponedorCorrespondencia(cadenaProveedoresIa, json);
    }
}
