package mx.uv.taan.reportes.dominio;

import mx.uv.taan.comun.error.ReglaNegocioException;

/** Se lanza cuando se intenta un cambio de estado que no existe en el diagrama de estados (Anexo G). */
public class TransicionInvalidaException extends ReglaNegocioException {

    public TransicionInvalidaException(EstadoReporte origen, EstadoReporte destino) {
        super(409, "TRANSICION_INVALIDA",
                "El reporte no puede pasar de «" + origen.textoCiudadano() + "» a «" + destino.textoCiudadano() + "».");
    }
}
