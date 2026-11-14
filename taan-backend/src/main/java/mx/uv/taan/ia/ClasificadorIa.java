package mx.uv.taan.ia;

import java.util.List;

/**
 * Adaptador de IA (Anexo E). Cambiar de proveedor es cambiar la implementación,
 * sin tocar el resto del sistema. Solo recibe la descripción: nunca nombre ni teléfono.
 */
public interface ClasificadorIa {

    /**
     * @param descripcion          texto del ciudadano
     * @param categoriasPermitidas catálogo vigente ("Categoría / Subcategoría")
     * @throws RuntimeException si el proveedor falla o su respuesta no es válida
     */
    ResultadoClasificacion clasificar(String descripcion, List<String> categoriasPermitidas);
}
