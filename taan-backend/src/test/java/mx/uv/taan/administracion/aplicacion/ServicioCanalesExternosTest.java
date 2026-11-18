package mx.uv.taan.administracion.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import mx.uv.taan.administracion.dominio.CanalExterno;
import mx.uv.taan.administracion.dominio.NivelParticipacion;
import mx.uv.taan.administracion.dominio.Organizacion;
import mx.uv.taan.administracion.dominio.TipoCanal;
import mx.uv.taan.administracion.infraestructura.CanalExternoRepository;
import mx.uv.taan.administracion.infraestructura.OrganizacionRepository;
import mx.uv.taan.comun.error.ReglaNegocioException;

class ServicioCanalesExternosTest {

    private ServicioCanalesExternos servicio;

    @BeforeEach
    void preparar() {
        CanalExternoRepository canales = mock(CanalExternoRepository.class);
        OrganizacionRepository organizaciones = mock(OrganizacionRepository.class);
        servicio = new ServicioCanalesExternos(canales, organizaciones);

        when(organizaciones.findById(1L)).thenReturn(Optional.of(
                new Organizacion("Ayuntamiento de Ixtaczoquitlán", NivelParticipacion.integrada, 15)));
        when(organizaciones.findById(2L)).thenReturn(Optional.of(
                new Organizacion("CFE", NivelParticipacion.externa, 15)));
        when(canales.save(any())).thenAnswer(i -> {
            CanalExterno c = i.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 8L);
            return c;
        });
    }

    @Test
    void registraElCanalDeUnaOrganizacionExterna() {
        CanalExternoVista canal = servicio.crear(2L, TipoCanal.telefono, " 071 ", "Sitio oficial de CFE");

        assertEquals("CFE", canal.organizacion());
        assertEquals("071", canal.valor());
    }

    @Test
    void unaOrganizacionIntegradaNoTieneCanalesExternos() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(1L, TipoCanal.telefono, "2721234567", "Directorio"));

        assertEquals("ORGANIZACION_NO_EXTERNA", error.getCodigo());
    }

    @Test
    void elValorDebeCorresponderAlTipo() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(2L, TipoCanal.sitio_web, "cfe.mx sin protocolo", "Directorio"));

        assertEquals(400, error.getEstadoHttp());
    }
}
