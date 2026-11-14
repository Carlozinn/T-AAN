package mx.uv.taan.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.Test;

class ClasificadorConRespaldoTest {

    private static final List<String> CATALOGO = List.of("Vialidades / Bache");
    private static final ClasificadorIa FALLA = (d, c) -> { throw new IllegalStateException("caído"); };
    private static final ClasificadorIa BACHE = (d, c) -> new ResultadoClasificacion("Vialidades", "Bache", false);

    @Test
    void usaElPrincipalCuandoResponde() {
        assertEquals("Vialidades", new ClasificadorConRespaldo(BACHE, FALLA).clasificar("hay un bache", CATALOGO).categoria());
    }

    @Test
    void usaElRespaldoCuandoElPrincipalFalla() {
        assertEquals("Bache", new ClasificadorConRespaldo(FALLA, BACHE).clasificar("hay un bache", CATALOGO).subcategoria());
    }

    @Test
    void siAmbosFallanQuedaSinClasificacion() {
        assertFalse(new ClasificadorConRespaldo(FALLA, FALLA).clasificar("hay un bache", CATALOGO).clasificado());
    }
}
