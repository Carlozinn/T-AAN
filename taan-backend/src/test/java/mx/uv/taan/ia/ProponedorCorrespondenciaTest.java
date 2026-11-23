package mx.uv.taan.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class ProponedorCorrespondenciaTest {

    private static final List<ProponedorCorrespondencia.AreaParaIa> AREAS = List.of(
            new ProponedorCorrespondencia.AreaParaIa(4L, "Obras Públicas", "Bacheo, banquetas y vialidades"),
            new ProponedorCorrespondencia.AreaParaIa(1L, "Alumbrado Público", null));
    private static final List<ProponedorCorrespondencia.CategoriaParaIa> CATEGORIAS = List.of(
            new ProponedorCorrespondencia.CategoriaParaIa(12L, "Vialidades / Bache"),
            new ProponedorCorrespondencia.CategoriaParaIa(1L, "Alumbrado público"),
            new ProponedorCorrespondencia.CategoriaParaIa(10L, "Limpia / Basura acumulada"));

    private static Map<Long, Long> proponer(ProveedorIa... proveedores) {
        return new ProponedorCorrespondencia(new CadenaProveedores(List.of(proveedores)), JsonMapper.builder().build())
                .proponer(AREAS, CATEGORIAS);
    }

    @Test
    void devuelveSoloParesConIdsEnviados() {
        Map<Long, Long> propuesta = proponer(ProveedorFalso.queResponde("gemini", """
                {"asignaciones":[{"categoriaId":12,"areaId":4},{"categoriaId":1,"areaId":1},
                  {"categoriaId":10,"areaId":null},{"categoriaId":99,"areaId":4},
                  {"categoriaId":1,"areaId":77},{"categoriaId":12,"areaId":1}]}"""));

        assertEquals(Map.of(12L, 4L, 1L, 1L), propuesta);
    }

    @Test
    void enviaNombresFuncionesYCategoriasDentroDelBloqueDeDatos() {
        ProveedorFalso gemini = ProveedorFalso.queResponde("gemini", "{\"asignaciones\":[]}");

        proponer(gemini);

        assertTrue(gemini.ultimosDatos.contains("id 4: Obras Públicas. Funciones: Bacheo, banquetas y vialidades"));
        assertTrue(gemini.ultimosDatos.contains("id 1: Alumbrado Público\n"));
        assertTrue(gemini.ultimosDatos.contains("id 12: Vialidades / Bache"));
        assertTrue(gemini.ultimosDatos.startsWith("<<<DATOS"));
    }

    @Test
    void unaRespuestaSinFormatoPasaAlRespaldo() {
        assertEquals(Map.of(12L, 4L), proponer(ProveedorFalso.queResponde("gemini", "{\"otra\":1}"),
                ProveedorFalso.queResponde("groq", "{\"asignaciones\":[{\"categoriaId\":12,\"areaId\":4}]}")));
    }

    @Test
    void siFallanTodosLanzaIaNoDisponible() {
        assertThrows(IaNoDisponibleException.class,
                () -> proponer(ProveedorFalso.queFalla("gemini"), ProveedorFalso.queFalla("groq")));
    }

    @Test
    void sinProveedoresConfiguradosTambienEsIaNoDisponible() {
        assertThrows(IaNoDisponibleException.class, () -> proponer(ProveedorFalso.sinClave("gemini")));
    }
}
