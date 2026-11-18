package mx.uv.taan.configuracion.aplicacion;

import java.util.List;

/** Esquema PlantillaArea del contrato: área estándar del art. 115 y las categorías que atiende. */
public record PlantillaAreaVista(String nombre, String descripcion, List<String> categorias) {
}
