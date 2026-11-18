package mx.uv.taan.administracion.aplicacion;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uv.taan.administracion.dominio.CanalExterno;
import mx.uv.taan.administracion.dominio.NivelParticipacion;
import mx.uv.taan.administracion.dominio.Organizacion;
import mx.uv.taan.administracion.dominio.TipoCanal;
import mx.uv.taan.administracion.infraestructura.CanalExternoRepository;
import mx.uv.taan.administracion.infraestructura.OrganizacionRepository;
import mx.uv.taan.comun.error.ReglaNegocioException;

/**
 * Canales oficiales de las organizaciones externas (CU-20, RF-036). Solo una organización con
 * nivel «externa» tiene canales: las integradas atienden dentro de T'AAN.
 */
@Service
public class ServicioCanalesExternos {

    private static final Map<TipoCanal, Pattern> FORMATOS = Map.of(
            // Desde 3 dígitos: los números cortos de servicio (071 de CFE, 911) también son oficiales.
            TipoCanal.telefono, Pattern.compile("^\\+?[0-9 ()-]{3,20}$"),
            TipoCanal.sitio_web, Pattern.compile("^https?://\\S+$"),
            TipoCanal.correo, Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"));

    private final CanalExternoRepository canales;
    private final OrganizacionRepository organizaciones;

    public ServicioCanalesExternos(CanalExternoRepository canales, OrganizacionRepository organizaciones) {
        this.canales = canales;
        this.organizaciones = organizaciones;
    }

    @Transactional(readOnly = true)
    public List<CanalExternoVista> listar() {
        Map<Long, String> nombres = organizaciones.findAll().stream()
                .collect(Collectors.toMap(Organizacion::getId, Organizacion::getNombre));
        return canales.findAllByOrderByOrganizacionIdAscIdAsc().stream()
                .map(c -> CanalExternoVista.de(c, nombres.get(c.getOrganizacionId())))
                .toList();
    }

    @Transactional
    public CanalExternoVista crear(long organizacionId, TipoCanal tipo, String valor, String fuenteVerificacion) {
        Organizacion organizacion = organizaciones.findById(organizacionId)
                .filter(o -> o.getNivelParticipacion() == NivelParticipacion.externa)
                .orElseThrow(() -> ReglaNegocioException.noProcesable("ORGANIZACION_NO_EXTERNA",
                        "La organización no existe o no es externa."));
        String limpio = valorValido(tipo, valor);
        CanalExterno canal = canales.save(new CanalExterno(organizacionId, tipo, limpio, fuenteVerificacion.strip()));
        return CanalExternoVista.de(canal, organizacion.getNombre());
    }

    /** PATCH: null = sin cambio; el valor se valida contra el tipo resultante. */
    @Transactional
    public CanalExternoVista actualizar(long id, TipoCanal tipo, String valor, String fuenteVerificacion,
                                        Boolean activo) {
        CanalExterno canal = canales.findById(id).orElseThrow(ReglaNegocioException::noEncontrado);
        if (tipo != null || valor != null) {
            TipoCanal nuevoTipo = tipo != null ? tipo : canal.getTipo();
            String nuevoValor = valor != null ? valor : canal.getValor();
            canal.cambiarCanal(nuevoTipo, valorValido(nuevoTipo, nuevoValor));
        }
        if (fuenteVerificacion != null) {
            canal.cambiarFuente(fuenteVerificacion.strip());
        }
        if (activo != null) {
            canal.cambiarActivo(activo);
        }
        String nombre = organizaciones.findById(canal.getOrganizacionId())
                .map(Organizacion::getNombre).orElse(null);
        return CanalExternoVista.de(canal, nombre);
    }

    /** El ciudadano verá este valor como enlace o teléfono: debe tener el formato de su tipo. */
    private static String valorValido(TipoCanal tipo, String valor) {
        String limpio = valor.strip();
        boolean valido = !limpio.isEmpty() && FORMATOS.getOrDefault(tipo, Pattern.compile(".+"))
                .matcher(limpio).matches();
        if (!valido) {
            throw new ReglaNegocioException(400, "VALIDACION",
                    "El valor no tiene el formato de un canal de tipo " + tipo + ".");
        }
        return limpio;
    }
}
