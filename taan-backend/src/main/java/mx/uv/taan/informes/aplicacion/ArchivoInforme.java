package mx.uv.taan.informes.aplicacion;

/** Informe listo para descargar. */
public record ArchivoInforme(byte[] contenido, String tipoContenido, String nombreArchivo) {
}
