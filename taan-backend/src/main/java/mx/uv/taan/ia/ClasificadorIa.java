package mx.uv.taan.ia;

import java.util.List;

/**
 * Clasificación de reportes con IA (Anexo E). Solo recibe la descripción: nunca nombre ni teléfono.
 */
public interface ClasificadorIa {

    /**
     * @param descripcion          texto del ciudadano
     * @param categoriasPermitidas catálogo vigente («Categoría» y «Categoría / Subcategoría»)
     * @return la clasificación validada contra el catálogo, o sin clasificación si la IA no encontró
     *         categoría o no hay ningún proveedor configurado
     * @throws IaNoDisponibleException si los proveedores configurados fallaron: el reporte se reintenta
     */
    ResultadoClasificacion clasificar(String descripcion, List<String> categoriasPermitidas);
}
