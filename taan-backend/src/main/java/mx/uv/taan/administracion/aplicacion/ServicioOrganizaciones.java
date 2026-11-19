package mx.uv.taan.administracion.aplicacion;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uv.taan.administracion.dominio.NivelParticipacion;
import mx.uv.taan.administracion.dominio.Organizacion;
import mx.uv.taan.administracion.infraestructura.OrganizacionRepository;
import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.configuracion.aplicacion.AreaVista;
import mx.uv.taan.configuracion.aplicacion.ServicioAreas;
import mx.uv.taan.cuentas.aplicacion.CredencialTemporal;
import mx.uv.taan.cuentas.aplicacion.ServicioCuentasInstitucionales;
import mx.uv.taan.cuentas.dominio.Rol;

/**
 * Registro de organizaciones por el administrador (CU-11, RF-027), su cuenta inicial de
 * coordinación (CU-12) y la carga de la plantilla municipal (CU-22). Las cuentas y las áreas
 * se crean a través de los servicios de sus módulos.
 */
@Service
public class ServicioOrganizaciones {

    private static final int PLAZO_REAPERTURA_POR_DEFECTO = 15;

    private final OrganizacionRepository organizaciones;
    private final ServicioCuentasInstitucionales cuentas;
    private final ServicioAreas areas;

    public ServicioOrganizaciones(OrganizacionRepository organizaciones, ServicioCuentasInstitucionales cuentas,
                                  ServicioAreas areas) {
        this.organizaciones = organizaciones;
        this.cuentas = cuentas;
        this.areas = areas;
    }

    @Transactional(readOnly = true)
    public List<OrganizacionVista> listar() {
        return organizaciones.findAllByOrderByNombre().stream().map(OrganizacionVista::de).toList();
    }

    /** Una organización por id (para mostrar su nombre o su plazo de reapertura). */
    @Transactional(readOnly = true)
    public java.util.Optional<OrganizacionVista> consultar(long id) {
        return organizaciones.findById(id).map(OrganizacionVista::de);
    }

    @Transactional
    public OrganizacionVista crear(String nombre, NivelParticipacion nivel, Integer plazoReaperturaDias) {
        String limpio = nombre.strip();
        if (organizaciones.existsByNombreIgnoreCase(limpio)) {
            throw nombreDuplicado();
        }
        int plazo = plazoReaperturaDias != null ? plazoReaperturaDias : PLAZO_REAPERTURA_POR_DEFECTO;
        return OrganizacionVista.de(organizaciones.save(new Organizacion(limpio, nivel, plazo)));
    }

    /** PATCH: null = sin cambio. Desactivar no borra historial (RF-027). */
    @Transactional
    public OrganizacionVista actualizar(long id, String nombre, NivelParticipacion nivel,
                                        Integer plazoReaperturaDias, Boolean activa) {
        Organizacion organizacion = buscar(id);
        if (nombre != null) {
            String limpio = nombre.strip();
            if (organizaciones.existsByNombreIgnoreCaseAndIdNot(limpio, id)) {
                throw nombreDuplicado();
            }
            organizacion.cambiarNombre(limpio);
        }
        if (nivel != null) {
            organizacion.cambiarNivelParticipacion(nivel);
        }
        if (plazoReaperturaDias != null) {
            organizacion.cambiarPlazoReapertura(plazoReaperturaDias);
        }
        if (activa != null) {
            organizacion.cambiarActiva(activa);
        }
        return OrganizacionVista.de(organizacion);
    }

    /** Cuenta inicial de coordinación con credencial temporal (CU-12). */
    @Transactional
    public CredencialTemporal crearCoordinacion(long id, String nombre, String correo) {
        buscar(id);
        return cuentas.crear(nombre, correo, Rol.coordinacion, id, null);
    }

    /** Crea las áreas estándar de la plantilla municipal en la organización (CU-22, paso 1). */
    @Transactional
    public List<AreaVista> aplicarPlantilla(long id) {
        buscar(id);
        return areas.aplicarPlantilla(id);
    }

    private Organizacion buscar(long id) {
        return organizaciones.findById(id).orElseThrow(ReglaNegocioException::noEncontrado);
    }

    private static ReglaNegocioException nombreDuplicado() {
        return ReglaNegocioException.conflicto("NOMBRE_DUPLICADO", "Ya existe una organización con ese nombre.");
    }
}
