package mx.uv.taan.ia;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Proveedor principal: Google Gemini (modelo Flash-Lite).
 * PENDIENTE (incremento 5): llamar a la API de Gemini con RestClient pidiendo salida
 * estructurada en JSON con los campos de ResultadoClasificacion, validar que la categoría
 * exista en categoriasPermitidas y lanzar una excepción si la respuesta no es válida.
 */
@Component("clasificadorGemini")
public class ClasificadorGemini implements ClasificadorIa {

    private final String apiKey;
    private final String modelo;

    public ClasificadorGemini(@Value("${taan.ia.gemini.api-key}") String apiKey,
                              @Value("${taan.ia.gemini.modelo}") String modelo) {
        this.apiKey = apiKey;
        this.modelo = modelo;
    }

    @Override
    public ResultadoClasificacion clasificar(String descripcion, List<String> categoriasPermitidas) {
        throw new UnsupportedOperationException("Clasificación con Gemini pendiente de implementar (modelo " + modelo + ").");
    }
}
