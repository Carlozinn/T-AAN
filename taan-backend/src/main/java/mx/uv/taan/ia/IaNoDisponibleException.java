package mx.uv.taan.ia;

/** Ningún proveedor configurado respondió con un resultado válido: quien llama decide si reintenta. */
public class IaNoDisponibleException extends RuntimeException {

    public IaNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
