package mx.uv.taan.configuracion.aplicacion;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.cuentas.aplicacion.CredencialTemporal;
import mx.uv.taan.cuentas.aplicacion.ServicioCuentasInstitucionales;
import mx.uv.taan.cuentas.aplicacion.UsuarioActual;
import mx.uv.taan.cuentas.aplicacion.UsuarioInstitucional;
import mx.uv.taan.cuentas.dominio.Rol;

/**
 * Personal de la organización administrado por su coordinación (CU-12, RF-032). Aquí se
 * valida lo que es de configuración (que el área sea de la organización y esté activa); las
 * cuentas en sí las maneja el módulo cuentas.
 */
@Service
public class ServicioPersonal {

    private final ServicioCuentasInstitucionales cuentas;
    private final ServicioAreas areas;

    public ServicioPersonal(ServicioCuentasInstitucionales cuentas, ServicioAreas areas) {
        this.cuentas = cuentas;
        this.areas = areas;
    }

    public List<UsuarioInstitucional> listar(UsuarioActual coordinacion) {
        return cuentas.listarDeOrganizacion(coordinacion.organizacionId());
    }

    @Transactional
    public CredencialTemporal crear(UsuarioActual coordinacion, String nombre, String correo, Rol rol, Long areaId) {
        exigirRolDelPersonal(rol);
        if (rol == Rol.personal_area) {
            if (areaId == null) {
                throw ReglaNegocioException.noProcesable("AREA_REQUERIDA",
                        "El personal de área necesita un área asignada.");
            }
            areas.exigirActiva(coordinacion.organizacionId(), areaId);
        }
        return cuentas.crear(nombre, correo, rol, coordinacion.organizacionId(), areaId);
    }

    @Transactional
    public UsuarioInstitucional actualizar(UsuarioActual coordinacion, long usuarioId,
                                           String nombre, Rol rol, Long areaId, Boolean activo) {
        if (rol != null) {
            exigirRolDelPersonal(rol);
        }
        if (areaId != null) {
            areas.exigirActiva(coordinacion.organizacionId(), areaId);
        }
        return cuentas.actualizar(coordinacion, usuarioId, nombre, rol, areaId, activo);
    }

    public CredencialTemporal restablecerCredencial(UsuarioActual coordinacion, long usuarioId) {
        return cuentas.restablecerCredencial(coordinacion.organizacionId(), usuarioId);
    }

    /** La coordinación solo administra coordinación y personal de área (contrato: UsuarioNuevo.rol). */
    private static void exigirRolDelPersonal(Rol rol) {
        if (rol != Rol.coordinacion && rol != Rol.personal_area) {
            throw new ReglaNegocioException(400, "VALIDACION", "El rol debe ser coordinacion o personal_area.");
        }
    }
}
