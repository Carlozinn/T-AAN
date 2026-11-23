package mx.uv.taan.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Gemini contra un servidor simulado: nunca se llama a la API real. */
class ProveedorGeminiTest {

    private static final String URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-lite-latest:generateContent";

    private MockRestServiceServer servidor;
    private ProveedorGemini gemini;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        gemini = new ProveedorGemini(builder.build(), "clave-de-prueba", "gemini-flash-lite-latest");
    }

    @Test
    void pideJsonConEsquemaYDevuelveElTextoGenerado() {
        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "clave-de-prueba"))
                .andExpect(jsonPath("$.generationConfig.responseMimeType").value("application/json"))
                .andExpect(jsonPath("$.generationConfig.responseSchema.type").value("OBJECT"))
                .andExpect(jsonPath("$.systemInstruction.parts[0].text").value("instrucciones"))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value("datos"))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"{\\"categoria\\":\\"Vialidades\\"}"}]},
                          "finishReason":"STOP"}],"usageMetadata":{"totalTokenCount":42}}""",
                        MediaType.APPLICATION_JSON));

        String texto = gemini.generarJson("instrucciones", "datos", Map.of("type", "OBJECT"));

        assertEquals("{\"categoria\":\"Vialidades\"}", texto);
        servidor.verify();
    }

    @Test
    void sinCandidatosEsUnaFalla() {
        servidor.expect(requestTo(URL)).andRespond(withSuccess("{\"candidates\":[]}", MediaType.APPLICATION_JSON));

        assertThrows(IllegalStateException.class, () -> gemini.generarJson("i", "d", Map.of()));
    }

    @Test
    void unErrorDelServidorEsUnaFalla() {
        servidor.expect(requestTo(URL)).andRespond(withServerError());

        assertThrows(RuntimeException.class, () -> gemini.generarJson("i", "d", Map.of()));
    }

    @Test
    void unTiempoAgotadoEsUnaFalla() {
        servidor.expect(requestTo(URL)).andRespond(withException(new SocketTimeoutException("tiempo agotado")));

        assertThrows(RuntimeException.class, () -> gemini.generarJson("i", "d", Map.of()));
    }

    @Test
    void sinClaveNoEstaConfigurado() {
        assertFalse(new ProveedorGemini(RestClient.create(), "", "gemini-flash-lite-latest").configurado());
        assertFalse(new ProveedorGemini(RestClient.create(), null, "gemini-flash-lite-latest").configurado());
    }
}
