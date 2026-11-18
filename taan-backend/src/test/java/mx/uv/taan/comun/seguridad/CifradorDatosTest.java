package mx.uv.taan.comun.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Base64;

import org.junit.jupiter.api.Test;

class CifradorDatosTest {

    private static final String CLAVE = Base64.getEncoder().encodeToString(new byte[32]);

    private final CifradorDatos cifrador = new CifradorDatos(CLAVE);

    @Test
    void cifraYDescifraElTelefono() {
        String cifrado = cifrador.cifrar("2721234567");

        assertNotEquals("2721234567", cifrado);
        assertEquals("2721234567", cifrador.descifrar(cifrado));
    }

    @Test
    void elMismoTelefonoProduceCifradosDistintos() {
        assertNotEquals(cifrador.cifrar("2721234567"), cifrador.cifrar("2721234567"));
    }

    @Test
    void otraClaveNoPuedeDescifrar() {
        byte[] otra = new byte[32];
        otra[0] = 1;
        CifradorDatos ajeno = new CifradorDatos(Base64.getEncoder().encodeToString(otra));

        assertThrows(IllegalStateException.class, () -> ajeno.descifrar(cifrador.cifrar("2721234567")));
    }

    @Test
    void rechazaUnaClaveDeLongitudIncorrecta() {
        assertThrows(IllegalStateException.class,
                () -> new CifradorDatos(Base64.getEncoder().encodeToString(new byte[16])));
    }
}
