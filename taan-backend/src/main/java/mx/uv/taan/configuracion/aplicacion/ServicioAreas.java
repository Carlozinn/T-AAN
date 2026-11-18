package mx.uv.taan.configuracion.aplicacion;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.configuracion.dominio.Area;
import mx.uv.taan.configuracion.infraestructura.AreaRepository;
import mx.uv.taan.configuracion.infraestructura.PlantillaRepository;
import mx.uv.taan.cuentas.aplicacion.ServicioCuentasInstitucionales;

/**
 * Áreas de cada organización (CU-13, RF-033) y carga de las áreas estándar de la plantilla
 * municipal (CU-22, RF-039). Toda operación recibe la organización: un área de otra
 * organización responde 404.
 */
@Service
public class ServicioAreas {

    private final AreaRepository areas;
    private final PlantillaRepository plantilla;
    private final ServicioCuentasInstitucionales cuentas;

    public ServicioAreas(AreaRepository areas, PlantillaRepository plantilla, ServicioCuentasInstitucionales cuentas) {
        this.areas = areas;
        this.plantilla = plantilla;
        this.cuentas = cuentas;
    }

    @Transactional(readOnly = true)
    public List<AreaVista> listar(long organizacionId) {
        return areas.findByOrganizacionIdOrderByNombre(organizacionId).stream().map(AreaVista::de).toList();
    }

    /** Áreas activas: las que puede elegir el personal al asignar o reasignar (CU-07). */
    @Transactional(readOnly = true)
    public List<AreaVista> listarActivas(long organizacionId) {
        return areas.findByOrganizacionIdAndActivaTrueOrderByNombre(organizacionId).stream()
                .map(AreaVista::de).toList();
    }

    @Transactional
    public AreaVista crear(long organizacionId, String nombre, String telefonoPublico, String correoPublico) {
        String limpio = nombre.strip();
        if (areas.existsByOrganizacionIdAndNombreIgnoreCase(organizacionId, limpio)) {
            throw nombreDuplicado();
        }
        return AreaVista.de(areas.save(
                new Area(organizacionId, limpio, vacioANulo(telefonoPublico), vacioANulo(correoPublico))));
    }

    /**
     * PATCH: null = sin cambio; para borrar un contacto se envía cadena vacía. No se desactiva
     * un área con personal activo: primero hay que reasignarlo (409 AREA_CON_PERSONAL).
     */
    @Transactional
    public AreaVista actualizar(long organizacionId, long areaId, String nombre,
                                String telefonoPublico, String correoPublico, Boolean activa) {
        Area area = areas.findByIdAndOrganizacionId(areaId, organizacionId)
                .orElseThrow(ReglaNegocioException::noEncontrado);
        if (nombre != null) {
            String limpio = nombre.strip();
            if (areas.existsByOrganizacionIdAndNombreIgnoreCaseAndIdNot(organizacionId, limpio, areaId)) {
                throw nombreDuplicado();
            }
            area.cambiarNombre(limpio);
        }
        if (telefonoPublico != null || correoPublico != null) {
            area.cambiarContacto(
                    telefonoPublico != null ? vacioANulo(telefonoPublico) : area.getTelefonoPublico(),
                    correoPublico != null ? vacioANulo(correoPublico) : area.getCorreoPublico());
        }
        if (Boolean.FALSE.equals(activa) && area.isActiva() && cuentas.hayPersonalActivoEnArea(areaId)) {
            throw ReglaNegocioException.conflicto("AREA_CON_PERSONAL",
                    "El área tiene personal activo. Reasígnalo a otra área antes de desactivarla.");
        }
        if (activa != null) {
            area.cambiarActiva(activa);
        }
        return AreaVista.de(area);
    }

    /**
     * Crea en la organización las áreas estándar de la plantilla (CU-22, paso 1). Si ya
     * existe alguna con el mismo nombre, la plantilla se considera aplicada (409).
     */
    @Transactional
    public List<AreaVista> aplicarPlantilla(long organizacionId) {
        List<PlantillaAreaVista> estandar = plantilla.consultar();
        boolean yaAplicada = estandar.stream()
                .anyMatch(p -> areas.existsByOrganizacionIdAndNombreIgnoreCase(organizacionId, p.nombre()));
        if (yaAplicada) {
            throw ReglaNegocioException.conflicto("PLANTILLA_APLICADA",
                    "La organización ya tiene áreas de la plantilla municipal.");
        }
        return estandar.stream()
                .map(p -> AreaVista.de(areas.save(new Area(organizacionId, p.nombre(), null, null))))
                .toList();
    }

    /** Valida que el área exista, sea de la organización y esté activa (asignar personal, CU-12). */
    @Transactional(readOnly = true)
    public void exigirActiva(long organizacionId, long areaId) {
        areas.findByIdAndOrganizacionId(areaId, organizacionId)
                .filter(Area::isActiva)
                .orElseThrow(() -> ReglaNegocioException.noProcesable("AREA_INVALIDA",
                        "El área no existe en tu organización o está desactivada."));
    }

    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }

    private static ReglaNegocioException nombreDuplicado() {
        return ReglaNegocioException.conflicto("NOMBRE_DUPLICADO", "Ya existe un área con ese nombre.");
    }
}
