package mx.uv.taan.configuracion.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.configuracion.dominio.Area;
import mx.uv.taan.configuracion.infraestructura.AreaRepository;
import mx.uv.taan.configuracion.infraestructura.PlantillaRepository;
import mx.uv.taan.cuentas.aplicacion.ServicioCuentasInstitucionales;

class ServicioAreasTest {

    private static final long ORG = 1L;

    private AreaRepository areas;
    private PlantillaRepository plantilla;
    private ServicioCuentasInstitucionales cuentas;
    private ServicioAreas servicio;

    @BeforeEach
    void preparar() {
        areas = mock(AreaRepository.class);
        plantilla = mock(PlantillaRepository.class);
        cuentas = mock(ServicioCuentasInstitucionales.class);
        when(areas.save(any())).thenAnswer(i -> {
            Area a = i.getArgument(0);
            ReflectionTestUtils.setField(a, "id", 7L);
            return a;
        });
        servicio = new ServicioAreas(areas, plantilla, cuentas);
    }

    @Test
    void creaElAreaYConvierteLosContactosVaciosEnNulos() {
        AreaVista area = servicio.crear(ORG, "  Parques y Jardines ", "", null);

        assertEquals("Parques y Jardines", area.nombre());
        assertNull(area.telefonoPublico());
    }

    @Test
    void unNombreRepetidoEnLaOrganizacionResponde409() {
        when(areas.existsByOrganizacionIdAndNombreIgnoreCase(ORG, "Obras Públicas")).thenReturn(true);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(ORG, "Obras Públicas", null, null));

        assertEquals("NOMBRE_DUPLICADO", error.getCodigo());
    }

    @Test
    void noSeDesactivaUnAreaConPersonalActivo() {
        when(areas.findByIdAndOrganizacionId(7L, ORG)).thenReturn(Optional.of(area(7L)));
        when(cuentas.hayPersonalActivoEnArea(7L)).thenReturn(true);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.actualizar(ORG, 7L, null, null, null, false));

        assertEquals("AREA_CON_PERSONAL", error.getCodigo());
    }

    @Test
    void unAreaDeOtraOrganizacionResponde404() {
        when(areas.findByIdAndOrganizacionId(anyLong(), anyLong())).thenReturn(Optional.empty());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.actualizar(ORG, 50L, "Otro", null, null, null));

        assertEquals(404, error.getEstadoHttp());
    }

    @Test
    void laPlantillaCreaUnaAreaPorCadaAreaEstandar() {
        when(plantilla.consultar()).thenReturn(List.of(
                new PlantillaAreaVista("Alumbrado Público", "", List.of("Alumbrado público")),
                new PlantillaAreaVista("Obras Públicas", "", List.of("Vialidades"))));

        List<AreaVista> creadas = servicio.aplicarPlantilla(ORG);

        assertEquals(2, creadas.size());
        assertTrue(creadas.stream().allMatch(AreaVista::activa));
    }

    @Test
    void laPlantillaNoSeAplicaDosVeces() {
        when(plantilla.consultar()).thenReturn(List.of(
                new PlantillaAreaVista("Obras Públicas", "", List.of("Vialidades"))));
        when(areas.existsByOrganizacionIdAndNombreIgnoreCase(eq(ORG), anyString())).thenReturn(true);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> servicio.aplicarPlantilla(ORG));

        assertEquals("PLANTILLA_APLICADA", error.getCodigo());
        verify(areas, never()).save(any());
    }

    private static Area area(long id) {
        Area a = new Area(ORG, "Obras Públicas", null, null);
        ReflectionTestUtils.setField(a, "id", id);
        return a;
    }
}
