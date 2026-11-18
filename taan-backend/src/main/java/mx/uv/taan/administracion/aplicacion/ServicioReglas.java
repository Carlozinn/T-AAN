package mx.uv.taan.administracion.aplicacion;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uv.taan.administracion.dominio.Categoria;
import mx.uv.taan.administracion.dominio.Organizacion;
import mx.uv.taan.administracion.dominio.Regla;
import mx.uv.taan.administracion.infraestructura.CategoriaRepository;
import mx.uv.taan.administracion.infraestructura.OrganizacionRepository;
import mx.uv.taan.administracion.infraestructura.ReglaRepository;
import mx.uv.taan.administracion.infraestructura.ZonaRepository;
import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.configuracion.aplicacion.ServicioAreas;

/**
 * Reglas de canalización (CU-16, RF-030). Una regla solo puede apuntar a una organización con
 * cobertura en la zona, y su área (si la tiene) debe ser de esa organización y estar activa.
 */
@Service
public class ServicioReglas {

    static final ZoneId ZONA_HORARIA = ZoneId.of("America/Mexico_City");

    private final ReglaRepository reglas;
    private final ZonaRepository zonas;
    private final CategoriaRepository categorias;
    private final OrganizacionRepository organizaciones;
    private final ServicioAreas areas;

    public ServicioReglas(ReglaRepository reglas, ZonaRepository zonas, CategoriaRepository categorias,
                          OrganizacionRepository organizaciones, ServicioAreas areas) {
        this.reglas = reglas;
        this.zonas = zonas;
        this.categorias = categorias;
        this.organizaciones = organizaciones;
        this.areas = areas;
    }

    @Transactional(readOnly = true)
    public List<ReglaVista> listar(Long zonaId) {
        List<Regla> lista = zonaId == null
                ? reglas.findAllByOrderByZonaIdAscCategoriaIdAscPrioridadDesc()
                : reglas.findByZonaIdOrderByCategoriaIdAscPrioridadDesc(zonaId);
        return lista.stream().map(ReglaVista::de).toList();
    }

    @Transactional
    public ReglaVista crear(long zonaId, long categoriaId, long organizacionId, Long areaId,
                            Integer prioridad, LocalDate vigenteDesde, LocalDate vigenteHasta) {
        if (!zonas.existe(zonaId)) {
            throw ReglaNegocioException.noProcesable("ZONA_INVALIDA", "La zona no existe.");
        }
        categorias.findById(categoriaId).filter(Categoria::isActiva)
                .orElseThrow(() -> ReglaNegocioException.noProcesable("CATEGORIA_INVALIDA",
                        "La categoría no existe o está desactivada."));
        organizaciones.findById(organizacionId).filter(Organizacion::isActiva)
                .orElseThrow(() -> ReglaNegocioException.noProcesable("ORGANIZACION_INVALIDA",
                        "La organización no existe o está desactivada."));
        if (!zonas.tieneCobertura(zonaId, organizacionId)) {
            throw ReglaNegocioException.noProcesable("SIN_COBERTURA",
                    "La organización no tiene cobertura en esa zona. Agrégala primero a la zona.");
        }
        if (areaId != null) {
            areas.exigirActiva(organizacionId, areaId);
        }
        LocalDate desde = vigenteDesde != null ? vigenteDesde : LocalDate.now(ZONA_HORARIA);
        exigirVigenciaCoherente(desde, vigenteHasta);
        if (reglas.existsByZonaIdAndCategoriaIdAndOrganizacionIdAndActivaTrue(zonaId, categoriaId, organizacionId)) {
            throw ReglaNegocioException.noProcesable("REGLA_DUPLICADA",
                    "Ya existe una regla activa para esa zona, categoría y organización. Edítala en su lugar.");
        }
        return ReglaVista.de(reglas.save(new Regla(zonaId, categoriaId, organizacionId, areaId,
                prioridad != null ? prioridad : 0, desde, vigenteHasta)));
    }

    /** PATCH: null = sin cambio (incluidos areaId y vigenteHasta). */
    @Transactional
    public ReglaVista actualizar(long id, Integer prioridad, Long areaId, LocalDate vigenteHasta, Boolean activa) {
        Regla regla = reglas.findById(id).orElseThrow(ReglaNegocioException::noEncontrado);
        if (prioridad != null) {
            regla.cambiarPrioridad(prioridad);
        }
        if (areaId != null) {
            areas.exigirActiva(regla.getOrganizacionId(), areaId);
            regla.cambiarArea(areaId);
        }
        if (vigenteHasta != null) {
            exigirVigenciaCoherente(regla.getVigenteDesde(), vigenteHasta);
            regla.cerrarVigencia(vigenteHasta);
        }
        if (activa != null) {
            regla.cambiarActiva(activa);
        }
        return ReglaVista.de(regla);
    }

    private static void exigirVigenciaCoherente(LocalDate desde, LocalDate hasta) {
        if (hasta != null && hasta.isBefore(desde)) {
            throw ReglaNegocioException.noProcesable("VIGENCIA_INVALIDA",
                    "La vigencia no puede terminar antes de empezar.");
        }
    }
}
