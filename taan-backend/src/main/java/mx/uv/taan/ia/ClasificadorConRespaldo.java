package mx.uv.taan.ia;

import java.util.List;

/**
 * Usa el proveedor principal (Gemini) y, si falla, el de respaldo (Groq).
 * Si ambos fallan devuelve "sin clasificación": el reporte llega a la organización
 * receptora por defecto con motivo sin_clasificacion (RF-037), nunca se pierde.
 * No depende de Spring; la conexión con los proveedores se hace en ConfiguracionIa.
 */
public class ClasificadorConRespaldo implements ClasificadorIa {

    private final ClasificadorIa principal;
    private final ClasificadorIa respaldo;

    public ClasificadorConRespaldo(ClasificadorIa principal, ClasificadorIa respaldo) {
        this.principal = principal;
        this.respaldo = respaldo;
    }

    @Override
    public ResultadoClasificacion clasificar(String descripcion, List<String> categoriasPermitidas) {
        try {
            return principal.clasificar(descripcion, categoriasPermitidas);
        } catch (RuntimeException errorPrincipal) {
            try {
                return respaldo.clasificar(descripcion, categoriasPermitidas);
            } catch (RuntimeException errorRespaldo) {
                return ResultadoClasificacion.sinClasificacion();
            }
        }
    }
}
