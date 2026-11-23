package mx.uv.taan.ia;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Google Gemini (Flash-Lite), proveedor principal. Pide salida JSON con responseSchema, así el
 * modelo no puede responder fuera del formato. La clave viaja en el encabezado x-goog-api-key,
 * nunca en la URL, para que no aparezca en logs de acceso.
 */
class ProveedorGemini implements ProveedorIa {

    private static final String URL = "https://generativelanguage.googleapis.com/v1beta/models/{modelo}:generateContent";

    private final RestClient http;
    private final String apiKey;
    private final String modelo;

    ProveedorGemini(RestClient http, String apiKey, String modelo) {
        this.http = http;
        this.apiKey = apiKey;
        this.modelo = modelo;
    }

    @Override
    public String nombre() {
        return "gemini";
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
                "systemInstruction", Map.of("parts", List.of(Map.of("text", instrucciones))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", datos)))),
                "generationConfig", Map.of("responseMimeType", "application/json", "responseSchema", esquema,
                        "temperature", 0));
        Respuesta respuesta = http.post().uri(URL, modelo)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo)
                .retrieve()
                .body(Respuesta.class);
        if (respuesta == null || respuesta.candidates() == null || respuesta.candidates().isEmpty()
                || respuesta.candidates().get(0).content() == null
                || respuesta.candidates().get(0).content().parts() == null
                || respuesta.candidates().get(0).content().parts().isEmpty()) {
            throw new IllegalStateException("Gemini no devolvió contenido.");
        }
        return respuesta.candidates().get(0).content().parts().get(0).text();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Respuesta(List<Candidato> candidates) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Candidato(Contenido content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Contenido(List<Parte> parts) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Parte(String text) {
    }
}
