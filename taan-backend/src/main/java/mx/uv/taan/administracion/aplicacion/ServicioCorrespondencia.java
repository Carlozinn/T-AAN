package mx.uv.taan.administracion.aplicacion;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uv.taan.administracion.dominio.Categoria;
import mx.uv.taan.administracion.dominio.Regla;
import mx.uv.taan.administracion.infraestructura.CategoriaRepository;
import mx.uv.taan.administracion.infraestructura.ReglaRepository;
import mx.uv.taan.administracion.infraestructura.ZonaRepository;
import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.configuracion.aplicacion.AreaVista;
import mx.uv.taan.configuracion.aplicacion.PlantillaAreaVista;
import mx.uv.taan.configuracion.aplicacion.ServicioAreas;
import mx.uv.taan.configuracion.aplicacion.ServicioPlantilla;
import mx.uv.taan.ia.IaNoDisponibleException;
import mx.uv.taan.ia.ProponedorCorrespondencia;

/**
 * Correspondencia categoría → área de una organización (CU-22 pasos 3-5, RF-039).
 *
 * <p>No tiene tabla propia: la correspondencia son las reglas de la organización en su propio
 * territorio (las zonas donde es receptora por defecto). Confirmarla actualiza el área de esas
 * reglas, o las crea si aún no existen; ninguna regla cambia sin esta confirmación (EX-03).</p>
 */
@Service
public class ServicioCorrespondencia {

    /** Prioridad de las reglas que nacen de la correspondencia (la misma que las del piloto). */
    static final int PRIORIDAD_CORRESPONDENCIA = 10;

    private final ReglaRepository reglas;
    private final ZonaRepository zonas;
    private final CategoriaRepository categorias;
    private final ServicioAreas areas;
    private final ServicioPlantilla plantilla;
    private final ProponedorCorrespondencia proponedor;

    public ServicioCorrespondencia(ReglaRepository reglas, ZonaRepository zonas, CategoriaRepository categorias,
                                   ServicioAreas areas, ServicioPlantilla plantilla,
                                   ProponedorCorrespondencia proponedor) {
        this.reglas = reglas;
        this.zonas = zonas;
        this.categorias = categorias;
        this.areas = areas;
        this.plantilla = plantilla;
        this.proponedor = proponedor;
    }

    /** Elemento de la confirmación: areaId null deja la categoría sin área (la valida la coordinación). */
    public record Elemento(long categoriaId, Long areaId) {
    }

    @Transactional(readOnly = true)
    public List<CorrespondenciaVista> consultar(long organizacionId) {
        List<Long> territorio = zonas.zonasActivasConReceptora(organizacionId);
        Map<Long, Long> areaPorCategoria = new HashMap<>();
        reglas.findByOrganizacionIdAndActivaTrue(organizacionId).stream()
                .filter(r -> territorio.contains(r.getZonaId()))
                .filter(r -> r.getAreaId() != null)
                .forEach(r -> areaPorCategoria.putIfAbsent(r.getCategoriaId(), r.getAreaId()));

        Map<Long, String> nombresArea = areas.listar(organizacionId).stream()
                .collect(Collectors.toMap(AreaVista::id, AreaVista::nombre));
        Map<String, String> areaDePlantilla = areaDePlantillaPorCategoriaRaiz();
        Map<Long, Categoria> todas = categorias.findAll().stream()
                .collect(Collectors.toMap(Categoria::getId, Function.identity()));

        return categorias.findByActivaTrueOrderByNombre().stream()
                .sorted(Comparator.comparing((Categoria c) -> nombreRaiz(c, todas))
                        .thenComparing(c -> !c.esRaiz())
                        .thenComparing(Categoria::getNombre))
                .map(c -> {
                    Long areaId = areaPorCategoria.get(c.getId());
                    String area = areaId == null ? null : nombresArea.get(areaId);
                    boolean dePlantilla = area != null && area.equalsIgnoreCase(
                            areaDePlantilla.getOrDefault(nombreRaiz(c, todas), ""));
                    return new CorrespondenciaVista(c.getId(), c.getNombre(), areaId, area,
                            dePlantilla ? "plantilla" : "coordinacion");
                })
                .toList();
    }

    @Transactional
    public List<CorrespondenciaVista> confirmar(long organizacionId, List<Elemento> elementos) {
        for (Elemento e : elementos) {
            categorias.findById(e.categoriaId()).filter(Categoria::isActiva)
                    .orElseThrow(() -> ReglaNegocioException.noProcesable("CATEGORIA_INVALIDA",
                            "La categoría " + e.categoriaId() + " no existe o está desactivada."));
            if (e.areaId() != null) {
                areas.exigirActiva(organizacionId, e.areaId());
            }
        }
        List<Long> territorio = zonas.zonasActivasConReceptora(organizacionId);
        if (territorio.isEmpty()) {
            throw ReglaNegocioException.noProcesable("SIN_ZONA",
                    "Tu organización aún no tiene zona de cobertura. El administrador debe registrarla primero.");
        }
        LocalDate hoy = LocalDate.now(ServicioReglas.ZONA_HORARIA);
        for (Long zonaId : territorio) {
            for (Elemento e : elementos) {
                reglas.findFirstByZonaIdAndCategoriaIdAndOrganizacionIdAndActivaTrue(
                                zonaId, e.categoriaId(), organizacionId)
                        .ifPresentOrElse(
                                regla -> regla.cambiarArea(e.areaId()),
                                () -> reglas.save(new Regla(zonaId, e.categoriaId(), organizacionId, e.areaId(),
                                        PRIORIDAD_CORRESPONDENCIA, hoy, null)));
            }
        }
        return consultar(organizacionId);
    }

    /**
     * CU-22 paso 3: la IA propone un área para cada categoría según las funciones de las áreas
     * activas. No guarda nada: lo propuesto llega con origen «ia» y solo se activa con
     * {@link #confirmar} (EX-03). Sin transacción: no se mantiene una conexión abierta mientras
     * responde la IA.
     */
    public List<CorrespondenciaVista> proponer(long organizacionId) {
        List<AreaVista> activas = areas.listarActivas(organizacionId);
        if (activas.stream().allMatch(a -> a.funciones() == null)) {
            throw ReglaNegocioException.noProcesable("SIN_FUNCIONES",
                    "Captura las funciones de al menos un área para pedir la propuesta.");
        }
        List<CorrespondenciaVista> vigente = consultar(organizacionId);
        Map<Long, Categoria> todas = categorias.findAll().stream()
                .collect(Collectors.toMap(Categoria::getId, Function.identity()));
        Map<Long, Long> propuesta;
        try {
            propuesta = proponedor.proponer(
                    activas.stream().map(a -> new ProponedorCorrespondencia.AreaParaIa(a.id(), a.nombre(), a.funciones()))
                            .toList(),
                    vigente.stream().map(v -> new ProponedorCorrespondencia.CategoriaParaIa(v.categoriaId(),
                            nombreCompleto(todas.get(v.categoriaId()), todas))).toList());
        } catch (IaNoDisponibleException e) {
            throw ReglaNegocioException.noProcesable("IA_NO_DISPONIBLE",
                    "No se pudo obtener la propuesta en este momento. Tu correspondencia no cambió; "
                            + "puedes ajustarla manualmente o intentarlo más tarde.");
        }
        Map<Long, String> nombresArea = activas.stream().collect(Collectors.toMap(AreaVista::id, AreaVista::nombre));
        return vigente.stream()
                .map(v -> {
                    Long areaId = propuesta.get(v.categoriaId());
                    return areaId == null ? v
                            : new CorrespondenciaVista(v.categoriaId(), v.categoria(), areaId, nombresArea.get(areaId), "ia");
                })
                .toList();
    }

    /** «Categoría» o «Categoría / Subcategoría», para que la IA entienda el contexto de cada una. */
    private static String nombreCompleto(Categoria c, Map<Long, Categoria> todas) {
        return c.esRaiz() ? c.getNombre() : nombreRaiz(c, todas) + " / " + c.getNombre();
    }

    /** Nombre de la categoría raíz → nombre del área estándar que la atiende. */
    private Map<String, String> areaDePlantillaPorCategoriaRaiz() {
        Map<String, String> resultado = new HashMap<>();
        for (PlantillaAreaVista area : plantilla.consultar()) {
            area.categorias().forEach(categoria -> resultado.put(categoria, area.nombre()));
        }
        return resultado;
    }

    private static String nombreRaiz(Categoria c, Map<Long, Categoria> todas) {
        if (c.esRaiz()) {
            return c.getNombre();
        }
        Categoria padre = todas.get(c.getCategoriaPadreId());
        return padre == null ? c.getNombre() : padre.getNombre();
    }
}
