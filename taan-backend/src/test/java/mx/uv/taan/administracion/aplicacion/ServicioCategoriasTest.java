package mx.uv.taan.administracion.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import mx.uv.taan.administracion.dominio.Categoria;
import mx.uv.taan.administracion.infraestructura.CategoriaRepository;
import mx.uv.taan.comun.error.ReglaNegocioException;

class ServicioCategoriasTest {

    private CategoriaRepository categorias;
    private ServicioCategorias servicio;
    private Categoria alumbrado;
    private Categoria luminaria;

    @BeforeEach
    void preparar() {
        categorias = mock(CategoriaRepository.class);
        servicio = new ServicioCategorias(categorias);
        alumbrado = categoria(1L, "Alumbrado público", null);
        luminaria = categoria(5L, "Luminaria apagada", 1L);
        when(categorias.findById(1L)).thenReturn(Optional.of(alumbrado));
        when(categorias.findById(5L)).thenReturn(Optional.of(luminaria));
        when(categorias.findByCategoriaPadreIdIsNull()).thenReturn(List.of(alumbrado));
        when(categorias.findByCategoriaPadreId(1L)).thenReturn(List.of(luminaria));
        when(categorias.save(any())).thenAnswer(i -> {
            Categoria c = i.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 30L);
            return c;
        });
    }

    @Test
    void creaUnaSubcategoriaBajoUnaRaiz() {
        CategoriaVista nueva = servicio.crear("Poste inclinado", 1L);

        assertEquals(1L, nueva.categoriaPadreId());
    }

    @Test
    void noHayTercerNivel() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.crear("Foco fundido", 5L));

        assertEquals("CATEGORIA_PADRE_INVALIDA", error.getCodigo());
    }

    @Test
    void unaHermanaConElMismoNombreResponde409SinImportarMayusculas() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.crear("LUMINARIA APAGADA", 1L));

        assertEquals(409, error.getEstadoHttp());
    }

    @Test
    void elMismoNombreEnOtroNivelSiSePermite() {
        CategoriaVista nueva = servicio.crear("Luminaria apagada", null);

        assertEquals("Luminaria apagada", nueva.nombre());
    }

    @Test
    void renombrarseConSuPropioNombreNoEsConflicto() {
        CategoriaVista resultado = servicio.actualizar(5L, "Luminaria apagada", false);

        assertEquals(false, resultado.activa());
    }

    private static Categoria categoria(long id, String nombre, Long padre) {
        Categoria c = new Categoria(nombre, padre);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }
}
