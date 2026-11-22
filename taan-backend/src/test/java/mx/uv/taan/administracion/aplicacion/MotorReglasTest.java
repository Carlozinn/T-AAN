package mx.uv.taan.administracion.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
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
import mx.uv.taan.administracion.infraestructura.ZonaRepository.FilaZona;

class MotorReglasTest {

    private static final long ZONA = 1L;
    private static final long AYUNTAMIENTO = 1L;
    private static final long CFE = 2L;
    private static final long BACHE = 12L;
    private static final long VIALIDADES = 4L;

    private ZonaRepository zonas;
    private ReglaRepository reglas;
    private OrganizacionRepository organizaciones;
    private MotorReglas motor;
    private final LocalDate ayer = LocalDate.now(ServicioReglas.ZONA_HORARIA).minusDays(1);

    @BeforeEach
    void preparar() {
        zonas = mock(ZonaRepository.class);
        reglas = mock(ReglaRepository.class);
        CategoriaRepository categorias = mock(CategoriaRepository.class);
        organizaciones = mock(OrganizacionRepository.class);
        motor = new MotorReglas(zonas, reglas, categorias, organizaciones);

        when(zonas.zonaActivaQueContiene(anyDouble(), anyDouble()))
                .thenReturn(Optional.of(new FilaZona(ZONA, "Cabecera", AYUNTAMIENTO, true, null)));
        when(organizaciones.findById(AYUNTAMIENTO)).thenReturn(Optional.of(
                organizacion(AYUNTAMIENTO, "Ayuntamiento de Ixtaczoquitlán", NivelParticipacion.integrada)));
        when(organizaciones.findById(CFE)).thenReturn(Optional.of(organizacion(CFE, "CFE", NivelParticipacion.externa)));
        Categoria bache = new Categoria("Bache", VIALIDADES);
        ReflectionTestUtils.setField(bache, "id", BACHE);
        when(categorias.findById(BACHE)).thenReturn(Optional.of(bache));
    }

    @Test
    void sinCategoriaVaALaReceptora() {
        MotorReglas.Destino d = motor.determinar(18.85, -97.06, null).orElseThrow();

        assertEquals(AYUNTAMIENTO, d.organizacionId());
        assertEquals(MotorReglas.Motivo.sin_clasificacion, d.motivo());
        assertNull(d.areaId());
    }

    @Test
    void ganaLaReglaDeMayorPrioridadConSuArea() {
        when(reglas.findByZonaIdAndCategoriaIdAndActivaTrue(ZONA, BACHE)).thenReturn(List.of(
                regla(BACHE, AYUNTAMIENTO, 4L, 10, ayer, null),
                regla(BACHE, AYUNTAMIENTO, 5L, 5, ayer, null)));

        MotorReglas.Destino d = motor.determinar(18.85, -97.06, BACHE).orElseThrow();

        assertEquals(MotorReglas.Motivo.regla, d.motivo());
        assertEquals(4L, d.areaId());
        assertFalse(d.externa());
    }

    @Test
    void unEmpateEnLaPrioridadMaximaEquivaleANoTenerRegla() {
        when(reglas.findByZonaIdAndCategoriaIdAndActivaTrue(ZONA, BACHE)).thenReturn(List.of(
                regla(BACHE, AYUNTAMIENTO, 4L, 10, ayer, null),
                regla(BACHE, CFE, null, 10, ayer, null)));

        MotorReglas.Destino d = motor.determinar(18.85, -97.06, BACHE).orElseThrow();

        assertEquals(MotorReglas.Motivo.empate_reglas, d.motivo());
        assertEquals(AYUNTAMIENTO, d.organizacionId());
    }

    @Test
    void unaSubcategoriaSinReglasUsaLasDeSuCategoriaPadre() {
        when(reglas.findByZonaIdAndCategoriaIdAndActivaTrue(ZONA, VIALIDADES)).thenReturn(List.of(
                regla(VIALIDADES, AYUNTAMIENTO, 4L, 10, ayer, null)));

        MotorReglas.Destino d = motor.determinar(18.85, -97.06, BACHE).orElseThrow();

        assertEquals(MotorReglas.Motivo.regla, d.motivo());
        assertEquals(4L, d.areaId());
    }

    @Test
    void lasReglasVencidasOFuturasNoCuentan() {
        LocalDate hoy = LocalDate.now(ServicioReglas.ZONA_HORARIA);
        when(reglas.findByZonaIdAndCategoriaIdAndActivaTrue(ZONA, BACHE)).thenReturn(List.of(
                regla(BACHE, AYUNTAMIENTO, 4L, 10, hoy.minusDays(30), hoy.minusDays(1)),
                regla(BACHE, AYUNTAMIENTO, 4L, 10, hoy.plusDays(5), null)));

        MotorReglas.Destino d = motor.determinar(18.85, -97.06, BACHE).orElseThrow();

        assertEquals(MotorReglas.Motivo.sin_regla, d.motivo());
    }

    @Test
    void unaOrganizacionExternaVaSinArea() {
        when(reglas.findByZonaIdAndCategoriaIdAndActivaTrue(ZONA, BACHE)).thenReturn(List.of(
                regla(BACHE, CFE, 4L, 20, ayer, null)));

        MotorReglas.Destino d = motor.determinar(18.85, -97.06, BACHE).orElseThrow();

        assertTrue(d.externa());
        assertEquals(CFE, d.organizacionId());
        assertNull(d.areaId());
    }

    @Test
    void fueraDeTodaZonaNoHayDestino() {
        when(zonas.zonaActivaQueContiene(anyDouble(), anyDouble())).thenReturn(Optional.empty());

        assertTrue(motor.determinar(19.43, -99.13, BACHE).isEmpty());
    }

    @Test
    void unaReglaDeOrganizacionDesactivadaNoCuenta() {
        Organizacion inactiva = organizacion(9L, "Inactiva", NivelParticipacion.integrada);
        inactiva.cambiarActiva(false);
        when(organizaciones.findById(9L)).thenReturn(Optional.of(inactiva));
        when(reglas.findByZonaIdAndCategoriaIdAndActivaTrue(ZONA, BACHE)).thenReturn(List.of(
                regla(BACHE, 9L, null, 50, ayer, null)));

        assertEquals(MotorReglas.Motivo.sin_regla, motor.determinar(18.85, -97.06, BACHE).orElseThrow().motivo());
    }

    private static Regla regla(long categoria, long organizacion, Long area, int prioridad, LocalDate desde, LocalDate hasta) {
        return new Regla(ZONA, categoria, organizacion, area, prioridad, desde, hasta);
    }

    private static Organizacion organizacion(long id, String nombre, NivelParticipacion nivel) {
        Organizacion o = new Organizacion(nombre, nivel, 15);
        ReflectionTestUtils.setField(o, "id", id);
        return o;
    }
}
