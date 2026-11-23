package mx.uv.taan.administracion.aplicacion;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uv.taan.administracion.dominio.NivelParticipacion;
import mx.uv.taan.administracion.dominio.Organizacion;
import mx.uv.taan.administracion.infraestructura.OrganizacionRepository;
import mx.uv.taan.administracion.infraestructura.ReglaRepository;
import mx.uv.taan.administracion.infraestructura.ZonaRepository;
import mx.uv.taan.administracion.infraestructura.ZonaRepository.FilaZona;
import mx.uv.taan.comun.error.ReglaNegocioException;
import tools.jackson.databind.ObjectMapper;

/**
 * Zonas de cobertura (CU-15; RF-029, RF-037) y verificación de cobertura de un punto (RF-014).
 *
 * <p>La organización receptora siempre forma parte de las organizaciones con cobertura de su
 * zona: se agrega sola al crear la zona, al cambiar de receptora y al redefinir la cobertura.</p>
 */
@Service
public class ServicioZonas {

    private final ZonaRepository zonas;
    private final OrganizacionRepository organizaciones;
    private final ReglaRepository reglas;
    private final ObjectMapper json;

    public ServicioZonas(ZonaRepository zonas, OrganizacionRepository organizaciones,
                         ReglaRepository reglas, ObjectMapper json) {
        this.zonas = zonas;
        this.organizaciones = organizaciones;
        this.reglas = reglas;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public List<ZonaVista> listar() {
        Map<Long, List<Long>> cobertura = zonas.organizacionesPorZona();
        return zonas.listar().stream()
                .map(f -> vista(f, cobertura.getOrDefault(f.id(), List.of())))
                .toList();
    }

    @Transactional
    public ZonaVista crear(String nombre, long organizacionReceptoraId, Map<String, Object> geometria) {
        String geoJson = geoJsonValido(geometria);
        exigirOrganizacionActiva(organizacionReceptoraId);
        long id = zonas.insertar(nombre.strip(), geoJson, organizacionReceptoraId);
        zonas.agregarOrganizacion(id, organizacionReceptoraId);
        return consultar(id);
    }

    /** PATCH: null = sin cambio. Redibujar valida la geometría nueva antes de guardarla (EX-01). */
    @Transactional
    public ZonaVista actualizar(long id, String nombre, Long organizacionReceptoraId,
                                Map<String, Object> geometria, Boolean activa) {
        String geoJson = geometria == null ? null : geoJsonValido(geometria);
        if (!zonas.existe(id)) {
            throw ReglaNegocioException.noEncontrado();
        }
        if (organizacionReceptoraId != null) {
            exigirOrganizacionActiva(organizacionReceptoraId);
        }
        zonas.actualizar(id, nombre == null ? null : nombre.strip(), organizacionReceptoraId, geoJson, activa);
        if (organizacionReceptoraId != null) {
            zonas.agregarOrganizacion(id, organizacionReceptoraId);
        }
        return consultar(id);
    }

    /**
     * Define quién tiene cobertura en la zona: a quién puede redirigirse un reporte (CU-21) y
     * quién puede tener reglas ahí. No se quita a una organización con reglas activas en la zona.
     */
    @Transactional
    public ZonaVista definirCobertura(long id, List<Long> organizacionIds) {
        FilaZona zona = zonas.buscar(id).orElseThrow(ReglaNegocioException::noEncontrado);
        Set<Long> nuevas = new LinkedHashSet<>(organizacionIds);
        nuevas.add(zona.organizacionReceptoraId());
        for (Long org : nuevas) {
            if (!organizaciones.existsById(org)) {
                throw new ReglaNegocioException(400, "ORGANIZACION_INVALIDA",
                        "La organización " + org + " no existe.");
            }
        }
        for (Long anterior : zonas.organizacionesDe(id)) {
            if (!nuevas.contains(anterior) && reglas.existsByZonaIdAndOrganizacionIdAndActivaTrue(id, anterior)) {
                throw new ReglaNegocioException(400, "REGLAS_ACTIVAS",
                        "La organización " + anterior + " tiene reglas activas en esta zona. Desactívalas primero.");
            }
        }
        zonas.reemplazarOrganizaciones(id, nuevas);
        return consultar(id);
    }

    /** ¿El punto está en una zona activa? Se consulta antes de enviar un reporte (CU-01 EX-05). */
    @Transactional(readOnly = true)
    public CoberturaVista cobertura(double latitud, double longitud) {
        if (latitud < -90 || latitud > 90 || longitud < -180 || longitud > 180) {
            throw new ReglaNegocioException(400, "VALIDACION", "La ubicación no es válida.");
        }
        return zonas.zonaQueContiene(latitud, longitud)
                .map(nombre -> new CoberturaVista(true, nombre))
                .orElseGet(() -> new CoberturaVista(false, null));
    }

    /** Organizaciones activas con cobertura en el punto, a las que puede redirigirse un reporte (CU-21). */
    @Transactional(readOnly = true)
    public List<OrganizacionResumen> organizacionesConCobertura(double latitud, double longitud) {
        return zonas.organizacionesActivasConCobertura(latitud, longitud).stream()
                .map(f -> new OrganizacionResumen(f.id(), f.nombre(), NivelParticipacion.valueOf(f.nivel())))
                .toList();
    }

    private ZonaVista consultar(long id) {
        FilaZona fila = zonas.buscar(id).orElseThrow(ReglaNegocioException::noEncontrado);
        return vista(fila, zonas.organizacionesDe(id));
    }

    @SuppressWarnings("unchecked")
    private ZonaVista vista(FilaZona f, List<Long> cobertura) {
        Map<String, Object> geometria = json.readValue(f.geoJson(), Map.class);
        return new ZonaVista(f.id(), f.nombre(), f.organizacionReceptoraId(), cobertura, f.activa(), geometria);
    }

    private String geoJsonValido(Map<String, Object> geometria) {
        String geoJson = json.writeValueAsString(geometria);
        if (!zonas.geometriaValida(geoJson)) {
            throw ReglaNegocioException.noProcesable("GEOMETRIA_INVALIDA",
                    "La geometría no es un Polygon o MultiPolygon válido en GeoJSON (longitud, latitud).");
        }
        return geoJson;
    }

    private void exigirOrganizacionActiva(long organizacionId) {
        organizaciones.findById(organizacionId)
                .filter(Organizacion::isActiva)
                .orElseThrow(() -> ReglaNegocioException.noProcesable("ORGANIZACION_INVALIDA",
                        "La organización receptora no existe o está desactivada."));
    }
}
