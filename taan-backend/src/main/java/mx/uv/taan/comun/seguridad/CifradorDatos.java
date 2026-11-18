package mx.uv.taan.comun.seguridad;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cifra datos personales en reposo con AES-256-GCM (RF-038: el teléfono «se almacena cifrado»).
 * Cada valor lleva su propio vector de inicialización aleatorio, así que el mismo teléfono
 * nunca produce el mismo texto cifrado; GCM además detecta cualquier alteración.
 *
 * <p>Vive en comun porque también lo usará el módulo de reportes, cuando el personal consulte
 * el teléfono del ciudadano de un reporte.</p>
 *
 * <p>Formato guardado: Base64(iv[12] || cifrado+etiqueta). Si se pierde la clave, los teléfonos
 * guardados ya no se pueden leer: guárdala como cualquier otro secreto.</p>
 */
@Component
public class CifradorDatos {

    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int BYTES_IV = 12;
    private static final int BITS_ETIQUETA = 128;

    private final SecretKey clave;
    private final SecureRandom azar = new SecureRandom();

    public CifradorDatos(@Value("${taan.cifrado.clave}") String claveBase64) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(claveBase64.strip());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("TAAN_CIFRADO_CLAVE debe estar en Base64.", e);
        }
        if (bytes.length != 32) {
            throw new IllegalStateException("TAAN_CIFRADO_CLAVE debe tener 32 bytes (256 bits) en Base64.");
        }
        this.clave = new SecretKeySpec(bytes, "AES");
    }

    public String cifrar(String textoClaro) {
        try {
            byte[] iv = new byte[BYTES_IV];
            azar.nextBytes(iv);
            Cipher cifrador = Cipher.getInstance(ALGORITMO);
            cifrador.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(BITS_ETIQUETA, iv));
            byte[] cifrado = cifrador.doFinal(textoClaro.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar el dato.", e);
        }
    }

    public String descifrar(String textoCifrado) {
        try {
            ByteBuffer datos = ByteBuffer.wrap(Base64.getDecoder().decode(textoCifrado));
            byte[] iv = new byte[BYTES_IV];
            datos.get(iv);
            byte[] cifrado = new byte[datos.remaining()];
            datos.get(cifrado);
            Cipher cifrador = Cipher.getInstance(ALGORITMO);
            cifrador.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(BITS_ETIQUETA, iv));
            return new String(cifrador.doFinal(cifrado), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("No se pudo descifrar el dato (¿cambió TAAN_CIFRADO_CLAVE?).", e);
        }
    }
}
