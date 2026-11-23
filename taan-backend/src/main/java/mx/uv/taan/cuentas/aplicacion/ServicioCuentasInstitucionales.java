package mx.uv.taan.cuentas.aplicacion;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.cuentas.dominio.Rol;
import mx.uv.taan.cuentas.dominio.Usuario;
import mx.uv.taan.cuentas.infraestructura.UsuarioRepository;

/**
 * Cuentas del personal institucional (CU-12; RF-003, RF-032). Es la única puerta a la tabla
 * usuario para los demás módulos: administración crea la coordinación inicial y la
 * coordinación administra a su personal, siempre a través de este servicio.
 *
 * <p>Todas las operaciones sobre una cuenta existente reciben la organización de quien las
 * pide: una cuenta de otra organización responde 404, sin revelar que existe.</p>
 */
@Service
public class ServicioCuentasInstitucionales {

    /** Sin caracteres que se confunden al dictarlos (0/O, 1/l/I). */
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int LARGO_TEMPORAL = 12;

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom azar = new SecureRandom();

    public ServicioCuentasInstitucionales(UsuarioRepository usuarios, PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
    }

    /** Identidad vigente de quien llama; una cuenta borrada o desactivada responde 401. */
    @Transactional(readOnly = true)
    public UsuarioActual identidadActiva(long usuarioId) {
        Usuario u = usuarios.findById(usuarioId)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new ReglaNegocioException(401, "NO_AUTENTICADO", "Vuelve a iniciar sesión."));
        return new UsuarioActual(u.getId(), u.getRol(), u.getOrganizacionId(), u.getAreaId());
    }

    @Transactional(readOnly = true)
    public List<UsuarioInstitucional> listarDeOrganizacion(long organizacionId) {
        return usuarios.findByOrganizacionIdOrderByNombre(organizacionId).stream()
                .map(UsuarioInstitucional::de)
                .toList();
    }

    /** Nombres del personal por id (para mostrar quién hizo cada cambio del historial). */
    @Transactional(readOnly = true)
    public Map<Long, String> nombres(Collection<Long> ids) {
        return usuarios.findAllById(ids).stream()
                .collect(Collectors.toMap(Usuario::getId, Usuario::getNombre));
    }

    /** true si alguna cuenta activa trabaja en el área (impide desactivarla sin reasignar antes). */
    @Transactional(readOnly = true)
    public boolean hayPersonalActivoEnArea(long areaId) {
        return usuarios.existsByAreaIdAndActivoTrue(areaId);
    }

    /**
     * Crea una cuenta de coordinación o de personal de área con credencial temporal. Quien
     * llama ya validó que la organización exista y que el área sea suya y esté activa.
     */
    @Transactional
    public CredencialTemporal crear(String nombre, String correo, Rol rol, long organizacionId, Long areaId) {
        if (rol != Rol.coordinacion && rol != Rol.personal_area) {
            throw new IllegalArgumentException("Solo se crean cuentas de coordinación o de personal de área.");
        }
        String correoNormalizado = ServicioRegistroCiudadano.normalizar(correo);
        if (usuarios.findByCorreo(correoNormalizado).isPresent()) {
            throw ReglaNegocioException.conflicto("CORREO_REGISTRADO",
                    "Ese correo ya pertenece a otra cuenta del personal.");
        }
        String temporal = generarTemporal();
        Usuario nuevo = Usuario.conCredencialTemporal(nombre.strip(), correoNormalizado,
                passwordEncoder.encode(temporal), rol, organizacionId,
                rol == Rol.personal_area ? areaId : null);
        return new CredencialTemporal(UsuarioInstitucional.de(usuarios.save(nuevo)), temporal);
    }

    /**
     * Edita, cambia de rol o área, o desactiva (PATCH: null = sin cambio). Nadie puede
     * desactivarse ni quitarse la coordinación a sí mismo, para no dejar la organización
     * sin quien la administre por accidente.
     */
    @Transactional
    public UsuarioInstitucional actualizar(UsuarioActual solicitante, long usuarioId,
                                           String nombre, Rol rol, Long areaId, Boolean activo) {
        Usuario u = deOrganizacion(solicitante.organizacionId(), usuarioId);
        boolean esSiMismo = u.getId() == solicitante.id();
        if (esSiMismo && (Boolean.FALSE.equals(activo) || (rol != null && rol != u.getRol()))) {
            throw ReglaNegocioException.noProcesable("OPERACION_SOBRE_SI_MISMO",
                    "No puedes desactivar tu propia cuenta ni cambiar tu propio rol.");
        }
        if (nombre != null) {
            u.cambiarNombre(nombre.strip());
        }
        if (rol != null || areaId != null) {
            Rol nuevoRol = rol != null ? rol : u.getRol();
            Long nuevaArea = areaId != null ? areaId : u.getAreaId();
            if (nuevoRol == Rol.personal_area && nuevaArea == null) {
                throw ReglaNegocioException.noProcesable("AREA_REQUERIDA",
                        "El personal de área necesita un área asignada.");
            }
            u.cambiarRol(nuevoRol, nuevaArea);
        }
        if (activo != null) {
            u.cambiarActivo(activo);
        }
        return UsuarioInstitucional.de(u);
    }

    /** Nueva credencial temporal (CU-04 FA-01: el personal recupera su acceso con su coordinación). */
    @Transactional
    public CredencialTemporal restablecerCredencial(long organizacionId, long usuarioId) {
        Usuario u = deOrganizacion(organizacionId, usuarioId);
        String temporal = generarTemporal();
        u.restablecerCredencial(passwordEncoder.encode(temporal));
        return new CredencialTemporal(UsuarioInstitucional.de(u), temporal);
    }

    private Usuario deOrganizacion(Long organizacionId, long usuarioId) {
        if (organizacionId == null) {
            throw ReglaNegocioException.noEncontrado();
        }
        return usuarios.findByIdAndOrganizacionId(usuarioId, organizacionId)
                .orElseThrow(ReglaNegocioException::noEncontrado);
    }

    private String generarTemporal() {
        StringBuilder texto = new StringBuilder(LARGO_TEMPORAL);
        for (int i = 0; i < LARGO_TEMPORAL; i++) {
            texto.append(ALFABETO.charAt(azar.nextInt(ALFABETO.length())));
        }
        return texto.toString();
    }
}
