package mx.uv.taan.cuentas.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.cuentas.dominio.Rol;
import mx.uv.taan.cuentas.dominio.Usuario;
import mx.uv.taan.cuentas.infraestructura.UsuarioRepository;

class ServicioCuentasInstitucionalesTest {

    private static final long ORG = 1L;

    private UsuarioRepository usuarios;
    private ServicioCuentasInstitucionales servicio;
    private UsuarioActual coordinacion;

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode(anyString())).thenAnswer(i -> "[hash:" + i.getArgument(0) + "]");
        when(usuarios.save(any())).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            ReflectionTestUtils.setField(u, "id", 20L);
            return u;
        });
        servicio = new ServicioCuentasInstitucionales(usuarios, passwordEncoder);
        coordinacion = new UsuarioActual(10L, Rol.coordinacion, ORG, null);
    }

    @Test
    void creaPersonalConCredencialTemporalLegible() {
        when(usuarios.findByCorreo("ana@ixtac.gob.mx")).thenReturn(Optional.empty());

        CredencialTemporal credencial = servicio.crear("Ana", " Ana@Ixtac.gob.mx", Rol.personal_area, ORG, 3L);

        assertEquals(12, credencial.passwordTemporal().length());
        assertTrue(credencial.passwordTemporal().matches("[A-HJ-NP-Za-km-z2-9]+"));
        assertEquals("ana@ixtac.gob.mx", credencial.usuario().correo());
        assertEquals(3L, credencial.usuario().areaId());
    }

    @Test
    void elCorreoRepetidoResponde409() {
        when(usuarios.findByCorreo("ana@ixtac.gob.mx")).thenReturn(Optional.of(usuario(21L, Rol.personal_area, 3L)));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.crear("Ana", "ana@ixtac.gob.mx", Rol.personal_area, ORG, 3L));

        assertEquals(409, error.getEstadoHttp());
        assertEquals("CORREO_REGISTRADO", error.getCodigo());
    }

    @Test
    void nadiePuedeDesactivarseASiMismo() {
        when(usuarios.findByIdAndOrganizacionId(10L, ORG)).thenReturn(Optional.of(usuario(10L, Rol.coordinacion, null)));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.actualizar(coordinacion, 10L, null, null, null, false));

        assertEquals("OPERACION_SOBRE_SI_MISMO", error.getCodigo());
    }

    @Test
    void pasarACoordinacionQuitaElArea() {
        Usuario ana = usuario(21L, Rol.personal_area, 3L);
        when(usuarios.findByIdAndOrganizacionId(21L, ORG)).thenReturn(Optional.of(ana));

        UsuarioInstitucional resultado = servicio.actualizar(coordinacion, 21L, null, Rol.coordinacion, null, null);

        assertEquals(Rol.coordinacion, resultado.rol());
        assertNull(resultado.areaId());
    }

    @Test
    void pasarAPersonalDeAreaSinAreaResponde422() {
        when(usuarios.findByIdAndOrganizacionId(22L, ORG)).thenReturn(Optional.of(usuario(22L, Rol.coordinacion, null)));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.actualizar(coordinacion, 22L, null, Rol.personal_area, null, null));

        assertEquals("AREA_REQUERIDA", error.getCodigo());
    }

    @Test
    void unaCuentaDeOtraOrganizacionResponde404() {
        when(usuarios.findByIdAndOrganizacionId(99L, ORG)).thenReturn(Optional.empty());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.restablecerCredencial(ORG, 99L));

        assertEquals(404, error.getEstadoHttp());
    }

    @Test
    void restablecerVuelveAExigirCambioDePassword() {
        Usuario ana = usuario(21L, Rol.personal_area, 3L);
        ana.cambiarPassword("[hash-propio]");
        when(usuarios.findByIdAndOrganizacionId(21L, ORG)).thenReturn(Optional.of(ana));

        assertFalse(ana.isCredencialTemporal());
        servicio.restablecerCredencial(ORG, 21L);

        assertTrue(ana.isCredencialTemporal());
    }

    private static Usuario usuario(long id, Rol rol, Long areaId) {
        Usuario u = Usuario.conCredencialTemporal("Persona", "p" + id + "@ixtac.gob.mx", "[hash]", rol, ORG, areaId);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }
}
