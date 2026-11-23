package mx.uv.taan.ia;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Groq, proveedor de respaldo (API compatible con OpenAI). Su modo JSON no recibe esquema: el
 * formato lo fijan las instrucciones y lo valida después el caso de uso.
 */
class ProveedorGroq implements ProveedorIa {

    private static final String URL = "https://api.groq.com/openai/v1/chat/completions";

    private final RestClient http;
    private final String apiKey;
    private final String modelo;

    ProveedorGroq(RestClient http, String apiKey, String modelo) {
        this.http = http;
        this.apiKey = apiKey;
        this.modelo = modelo;
    }

    @Override
    public String nombre() {
        return "groq";
    }

    @Override
    public String modelo() {
        return modelo;
    }

    @Override
    public boolean configurado() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String generarJson(String instrucciones, String datos, Map<String, Object> esquema) {
        Map<String, Object> cuerpo = Map.of(
                "model", modelo,
                "messages", List.of(Map.of("role", "system", "content", instrucciones),
                        Map.of("role", "user", "content", datos)),
                "response_format", Map.of("type", "json_object"),
                "temperature", 0);
        Respuesta respuesta = http.post().uri(URL)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo)
                .retrieve()
                .body(Respuesta.class);
        if (respuesta == null || respuesta.choices() == null || respuesta.choices().isEmpty()
                || respuesta.choices().get(0).message() == null) {
            throw new IllegalStateException("Groq no devolvió contenido.");
        }
        return respuesta.choices().get(0).message().content();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Respuesta(List<Opcion> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Opcion(Mensaje message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Mensaje(String content) {
    }
}
