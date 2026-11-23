package mx.uv.taan.ia;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prueba los proveedores en orden (Gemini y luego Groq) hasta obtener una respuesta válida. Una
 * respuesta que no pasa la validación del caso de uso cuenta como falla y activa el respaldo.
 */
class CadenaProveedores {

    private static final Logger log = LoggerFactory.getLogger(CadenaProveedores.class);

    private final List<ProveedorIa> proveedores;

    CadenaProveedores(List<ProveedorIa> proveedores) {
        this.proveedores = proveedores;
    }

    /**
     * @return vacío si ningún proveedor está configurado (no hay IA en este entorno)
     * @throws IaNoDisponibleException si todos los configurados fallaron
     */
    <T> Optional<T> primeraRespuestaValida(Function<ProveedorIa, T> intento) {
        List<ProveedorIa> configurados = proveedores.stream().filter(ProveedorIa::configurado).toList();
        if (configurados.isEmpty()) {
            return Optional.empty();
        }
        for (ProveedorIa proveedor : configurados) {
            try {
                return Optional.of(intento.apply(proveedor));
            } catch (RuntimeException e) {
                // Sin la traza ni el cuerpo de la petición: no deben llegar al log datos del reporte.
                log.warn("La IA {} ({}) no respondió con un resultado válido: {}", proveedor.nombre(),
                        proveedor.modelo(), e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
        throw new IaNoDisponibleException("Ningún proveedor de IA respondió con un resultado válido.");
    }
}
