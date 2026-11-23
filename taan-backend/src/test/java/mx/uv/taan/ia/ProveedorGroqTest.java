package mx.uv.taan.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Groq contra un servidor simulado: nunca se llama a la API real. */
class ProveedorGroqTest {

    private static final String URL = "https://api.groq.com/openai/v1/chat/completions";

    private MockRestServiceServer servidor;
    private ProveedorGroq groq;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        groq = new ProveedorGroq(builder.build(), "clave-de-prueba", "openai/gpt-oss-120b");
    }

    @Test
    void pideModoJsonYDevuelveElContenidoDelMensaje() {
        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer clave-de-prueba"))
                .andExpect(jsonPath("$.model").value("openai/gpt-oss-120b"))
                .andExpect(jsonPath("$.response_format.type").value("json_object"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].content").value("datos"))
                .andRespond(withSuccess("""
                        {"id":"x","choices":[{"index":0,"message":{"role":"assistant",
                          "content":"{\\"categoria\\":null}"},"finish_reason":"stop"}]}""",
                        MediaType.APPLICATION_JSON));

        assertEquals("{\"categoria\":null}", groq.generarJson("instrucciones", "datos", Map.of()));
        servidor.verify();
    }

    @Test
    void unLimiteDeUsoEsUnaFalla() {
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThrows(RuntimeException.class, () -> groq.generarJson("i", "d", Map.of()));
    }

    @Test
    void sinOpcionesEsUnaFalla() {
        servidor.expect(requestTo(URL)).andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));

        assertThrows(IllegalStateException.class, () -> groq.generarJson("i", "d", Map.of()));
    }
}
