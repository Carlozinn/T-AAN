package mx.uv.taan.administracion.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import mx.uv.taan.administracion.dominio.Categoria;
import mx.uv.taan.administracion.dominio.Regla;
import mx.uv.taan.administracion.infraestructura.CategoriaRepository;
import mx.uv.taan.administracion.infraestructura.ReglaRepository;
import mx.uv.taan.administracion.infraestructura.ZonaRepository;
import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.configuracion.aplicacion.AreaVista;
import mx.uv.taan.configuracion.aplicacion.PlantillaAreaVista;
import mx.uv.taan.configuracion.aplicacion.ServicioAreas;
import mx.uv.taan.configuracion.aplicacion.ServicioPlantilla;

class ServicioCorrespondenciaTest {

    private static final long ORG = 1L;

    private ReglaRepository reglas;
    private ZonaRepository zonas;
    private ServicioCorrespondencia servicio;
    private Categoria vialidades;
    private Categoria bache;

    @BeforeEach
    void preparar() {
        reglas = mock(ReglaRepository.class);
        zonas = mock(ZonaRepository.class);
        CategoriaRepository categorias = mock(CategoriaRepository.class);
        ServicioAreas areas = mock(ServicioAreas.class);
        ServicioPlantilla plantilla = mock(ServicioPlantilla.class);
        servicio = new ServicioCorrespondencia(reglas, zonas, categorias, areas, plantilla);

        vialidades = categoria(4L, "Vialidades", null);
        bache = categoria(12L, "Bache", 4L);
        when(categorias.findById(4L)).thenReturn(Optional.of(vialidades));
        when(categorias.findById(12L)).thenReturn(Optional.of(bache));
        when(categorias.findAll()).thenReturn(List.of(vialidades, bache));
        when(categorias.findByActivaTrueOrderByNombre()).thenReturn(List.of(bache, vialidades));
        when(areas.listar(ORG)).thenReturn(List.of(
                new AreaVista(4L, "Obras Públicas", null, null, true),
                new AreaVista(9L, "Bacheo", null, null, true)));
        when(plantilla.consultar()).thenReturn(List.of(
                new PlantillaAreaVista("Obras Públicas", "", List.of("Vialidades"))));
        when(zonas.zonasActivasConReceptora(ORG)).thenReturn(List.of(1L));
    }

    @Test
    void sinZonaPropiaNoSePuedeConfirmar() {
        when(zonas.zonasActivasConReceptora(ORG)).thenReturn(List.of());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.confirmar(ORG, List.of(new ServicioCorrespondencia.Elemento(12L, 9L))));

        assertEquals("SIN_ZONA", error.getCodigo());
        verify(reglas, never()).save(any());
    }

    @Test
    void confirmarCambiaElAreaDeLaReglaExistente() {
        Regla existente = new Regla(1L, 12L, ORG, 4L, 10, LocalDate.of(2026, 11, 1), null);
        when(reglas.findFirstByZonaIdAndCategoriaIdAndOrganizacionIdAndActivaTrue(1L, 12L, ORG))
                .thenReturn(Optional.of(existente));

        servicio.confirmar(ORG, List.of(new ServicioCorrespondencia.Elemento(12L, 9L)));

        assertEquals(9L, existente.getAreaId());
        verify(reglas, never()).save(any());
    }

    @Test
    void confirmarCreaLaReglaQueFaltaConPrioridadDeCorrespondencia() {
        when(reglas.findFirstByZonaIdAndCategoriaIdAndOrganizacionIdAndActivaTrue(1L, 12L, ORG))
                .thenReturn(Optional.empty());

        servicio.confirmar(ORG, List.of(new ServicioCorrespondencia.Elemento(12L, 9L)));

        ArgumentCaptor<Regla> nueva = ArgumentCaptor.forClass(Regla.class);
        verify(reglas).save(nueva.capture());
        assertEquals(ServicioCorrespondencia.PRIORIDAD_CORRESPONDENCIA, nueva.getValue().getPrioridad());
        assertEquals(9L, nueva.getValue().getAreaId());
    }

    @Test
    void elOrigenEsPlantillaSiElAreaCoincideConLaEstandar() {
        when(reglas.findByOrganizacionIdAndActivaTrue(ORG)).thenReturn(List.of(
                new Regla(1L, 4L, ORG, 4L, 10, LocalDate.of(2026, 11, 1), null),
                new Regla(1L, 12L, ORG, 9L, 10, LocalDate.of(2026, 11, 1), null)));

        List<CorrespondenciaVista> lista = servicio.consultar(ORG);

        assertEquals("Vialidades", lista.get(0).categoria());
        assertEquals("plantilla", lista.get(0).origen());
        assertEquals("Bache", lista.get(1).categoria());
        assertEquals("coordinacion", lista.get(1).origen());
    }

    private static Categoria categoria(long id, String nombre, Long padre) {
        Categoria c = new Categoria(nombre, padre);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }
}
