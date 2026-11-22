package mx.uv.taan.administracion.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import mx.uv.taan.administracion.dominio.NivelParticipacion;
import mx.uv.taan.administracion.dominio.Organizacion;
import mx.uv.taan.administracion.infraestructura.OrganizacionRepository;
import mx.uv.taan.configuracion.aplicacion.ServicioAreas;
import mx.uv.taan.cuentas.aplicacion.ServicioCuentasInstitucionales;

class ServicioOrganizacionesTest {

    private OrganizacionRepository organizaciones;
    private ServicioOrganizaciones servicio;

    @BeforeEach
    void preparar() {
        organizaciones = mock(OrganizacionRepository.class);
        servicio = new ServicioOrganizaciones(organizaciones, mock(ServicioCuentasInstitucionales.class),
                mock(ServicioAreas.class));
    }

    @Test
    void devuelveLosNombresDeVariasOrganizacionesEnUnaSolaConsulta() {
        Set<Long> ids = Set.of(1L, 2L);
        when(organizaciones.findAllById(ids)).thenReturn(List.of(organizacion(1L, "Xalapa"), organizacion(2L, "CFE")));

        assertEquals(Map.of(1L, "Xalapa", 2L, "CFE"), servicio.nombres(ids));
    }

    @Test
    void unIdInexistenteNoApareceEnElResultado() {
        when(organizaciones.findAllById(Set.of(99L))).thenReturn(List.of());

        assertTrue(servicio.nombres(Set.of(99L)).isEmpty());
    }

    private static Organizacion organizacion(long id, String nombre) {
        Organizacion o = new Organizacion(nombre, NivelParticipacion.integrada, 15);
        ReflectionTestUtils.setField(o, "id", id);
        return o;
    }
}
