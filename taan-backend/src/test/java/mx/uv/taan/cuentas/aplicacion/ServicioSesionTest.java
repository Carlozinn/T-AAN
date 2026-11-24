package mx.uv.taan.cuentas.aplicacion;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import mx.uv.taan.cuentas.dominio.Rol;
import mx.uv.taan.cuentas.infraestructura.CiudadanoRepository;
import mx.uv.taan.cuentas.infraestructura.ServicioTokens;
import mx.uv.taan.cuentas.infraestructura.UsuarioRepository;

class ServicioSesionTest {

    private ServicioDispositivos dispositivos;
    private ServicioSesion servicio;

    @BeforeEach
    void preparar() {
        dispositivos = mock(ServicioDispositivos.class);
        servicio = new ServicioSesion(mock(ServicioTokens.class), mock(CiudadanoRepository.class),
                mock(UsuarioRepository.class), dispositivos);
    }

    @Test
    void alCerrarSesionConTokenElCiudadanoDejaDeRecibirAvisosEnEseDispositivo() {
        servicio.cerrar(4L, Rol.ciudadano, "token-fcm");

        verify(dispositivos).darDeBaja(4L, "token-fcm");
    }

    @Test
    void sinTokenOSiendoPersonalNoTocaLosDispositivos() {
        servicio.cerrar(4L, Rol.ciudadano, " ");
        servicio.cerrar(4L, Rol.ciudadano, null);
        servicio.cerrar(2L, Rol.coordinacion, "token-fcm");

        verifyNoInteractions(dispositivos);
    }
}
