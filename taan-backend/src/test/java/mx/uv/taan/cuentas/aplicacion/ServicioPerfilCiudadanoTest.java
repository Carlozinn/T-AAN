package mx.uv.taan.cuentas.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.comun.seguridad.CifradorDatos;
import mx.uv.taan.cuentas.dominio.Ciudadano;
import mx.uv.taan.cuentas.infraestructura.CiudadanoRepository;

class ServicioPerfilCiudadanoTest {

    private CiudadanoRepository ciudadanos;
    private ServicioPerfilCiudadano servicio;
    private Ciudadano cuenta;

    @BeforeEach
    void preparar() {
        ciudadanos = mock(CiudadanoRepository.class);
        servicio = new ServicioPerfilCiudadano(ciudadanos,
                new CifradorDatos(Base64.getEncoder().encodeToString(new byte[32])));
        cuenta = Ciudadano.conCorreo("Vecina", "vecina@correo.mx", "[hash]");
        when(ciudadanos.findById(4L)).thenReturn(Optional.of(cuenta));
    }

    @Test
    void sinTelefonoElPerfilLoMuestraNuloYLaCuentaLoRequiere() {
        PerfilCiudadano perfil = servicio.consultar(4L);

        assertNull(perfil.telefono());
        assertNull(perfil.suspension());
        assertTrue(cuenta.requiereTelefono());
    }

    @Test
    void elTelefonoSeGuardaCifradoYSeMuestraClaro() {
        PerfilCiudadano perfil = servicio.actualizar(4L, null, "2721234567", null);

        assertEquals("2721234567", perfil.telefono());
        assertNotEquals("2721234567", cuenta.getTelefonoCifrado());
        assertFalse(cuenta.requiereTelefono());
        assertEquals("Vecina", perfil.nombre());
    }

    @Test
    void soloCambiaLoEnviado() {
        servicio.actualizar(4L, "  Vecina Corregida ", null, false);

        assertEquals("Vecina Corregida", cuenta.getNombre());
        assertFalse(cuenta.isAvisosCorreo());
        assertNull(cuenta.getTelefonoCifrado());
    }

    @Test
    void eliminarBorraLaCuenta() {
        servicio.eliminar(4L);

        verify(ciudadanos).delete(cuenta);
    }

    @Test
    void unaCuentaQueYaNoExisteResponde401() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> servicio.consultar(99L));
        assertEquals(401, error.getEstadoHttp());
    }
}
