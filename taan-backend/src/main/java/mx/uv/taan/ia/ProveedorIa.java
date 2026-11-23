package mx.uv.taan.ia;

import java.util.Map;

/**
 * Un servicio externo de IA que devuelve JSON (Anexo E: Gemini principal, Groq respaldo).
 * Cambiar de proveedor es agregar otra implementación, sin tocar los casos de uso.
 */
public interface ProveedorIa {

    String nombre();

    String modelo();

    /** Sin clave no hay proveedor: se salta sin contarlo como falla. */
    boolean configurado();

    /**
     * @param instrucciones qué debe hacer el modelo y el formato de salida
     * @param datos         lo que se analiza (texto del ciudadano o configuración); nunca datos personales
     * @param esquema       esquema de la respuesta en el formato de Gemini (OBJECT, STRING...); quien no lo
     *                      admita se guía por las instrucciones
     * @return el texto JSON que generó el modelo
     * @throws RuntimeException si el proveedor falla, tarda más del tiempo límite o no devuelve contenido
     */
    String generarJson(String instrucciones, String datos, Map<String, Object> esquema);
}
