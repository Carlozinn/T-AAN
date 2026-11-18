package mx.uv.taan.administracion.aplicacion;

/** Esquema Cobertura del contrato: zona es null cuando el punto está fuera de cobertura. */
public record CoberturaVista(boolean dentroDeCobertura, String zona) {
}
