package mx.uv.taan.administracion.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import mx.uv.taan.administracion.dominio.Categoria;
import mx.uv.taan.administracion.dominio.NivelParticipacion;
import mx.uv.taan.administracion.dominio.Organizacion;
import mx.uv.taan.administracion.dominio.Regla;
import mx.uv.taan.administracion.infraestructura.CategoriaRepository;
import mx.uv.taan.administracion.infraestructura.OrganizacionRepository;
import mx.uv.taan.administracion.infraestructura.ReglaRepository;
import mx.uv.taan.administracion.infraestructura.ZonaRepository;
import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.configuracion.aplicacion.ServicioAreas;

class ServicioReglasTest {

    private ReglaRepository reglas;
    private ZonaRepository zonas;
    private ServicioAreas areas;
    private ServicioReglas servicio;

    @BeforeEach
    void preparar() {
        reglas = mock(ReglaRepository.class);
        zonas = mock(ZonaRepository.class);
        CategoriaRepository categorias = mock(CategoriaRepository.class);
        OrganizacionRepository organizaciones = mock(OrganizacionRepository.class);
        areas = mock(ServicioAreas.class);
        servicio = new ServicioReglas(reglas, zonas, categorias, organizaciones, areas);

        when(zonas.existe(1L)).thenReturn(true);
        when(zonas.tieneCobertura(1L, 1L)).thenReturn(true);
        when(categorias.findById(2L)).thenReturn(Optional.of(new Categoria("Bache", 4L)));
        when(organizaciones.findById(1L)).thenReturn(Optional.of(
                new Organizacion("Ayuntamiento de Ixtaczoquitlán", NivelParticipacion.integrada, 15)));
        when(reglas.save(any())).thenAnswer(i -> {
            Regla r = i.getArgument(0);
            ReflectionTestUtils.setField(r, "id", 50L);
            return r;
        });
    }

    @Test
    void creaLaReglaConPrioridadCeroYVigenciaDesdeHoy() {
        ReglaVista regla = servicio.crear(1L, 2L, 1L, 4L, null, null, null);

        assertEquals(0, regla.prioridad());
        assertEquals(LocalDate.now(ServicioReglas.ZONA_HORARIA), regla.vigenteDesde());
        verify(areas).exigirActiva(1L, 4L);
    }

    @Test
    void sinCoberturaEnLaZonaNoHayRegla() {
        when(zonas.tieneCobertura(1L, 1L)).thenReturn(false);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(1L, 2L, 1L, null, 10, null, null));

        assertEquals("SIN_COBERTURA", error.getCodigo());
        verify(reglas, never()).save(any());
    }

    @Test
    void unAreaDeOtraOrganizacionSeRechaza() {
        doThrow(ReglaNegocioException.noProcesable("AREA_INVALIDA", "x")).when(areas).exigirActiva(1L, 99L);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(1L, 2L, 1L, 99L, 10, null, null));

        assertEquals("AREA_INVALIDA", error.getCodigo());
    }

    @Test
    void laVigenciaNoTerminaAntesDeEmpezar() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(1L, 2L, 1L, null, 10, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 11, 1)));

        assertEquals("VIGENCIA_INVALIDA", error.getCodigo());
    }

    @Test
    void noSeDuplicaUnaReglaActiva() {
        when(reglas.existsByZonaIdAndCategoriaIdAndOrganizacionIdAndActivaTrue(1L, 2L, 1L)).thenReturn(true);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(1L, 2L, 1L, null, 10, null, null));

        assertEquals("REGLA_DUPLICADA", error.getCodigo());
    }
}
