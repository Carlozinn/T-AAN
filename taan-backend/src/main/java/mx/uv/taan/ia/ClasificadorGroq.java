package mx.uv.taan.ia;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Proveedor de respaldo: Groq (modelos abiertos como Llama).
 * PENDIENTE (incremento 5): misma lógica que ClasificadorGemini, con la API de Groq.
 */
@Component("clasificadorGroq")
public class ClasificadorGroq implements ClasificadorIa {

    private final String apiKey;
    private final String modelo;

    public ClasificadorGroq(@Value("${taan.ia.groq.api-key}") String apiKey,
                            @Value("${taan.ia.groq.modelo}") String modelo) {
        this.apiKey = apiKey;
        this.modelo = modelo;
    }

    @Override
    public ResultadoClasificacion clasificar(String descripcion, List<String> categoriasPermitidas) {
        throw new UnsupportedOperationException("Clasificación con Groq pendiente de implementar (modelo " + modelo + ").");
    }
}
