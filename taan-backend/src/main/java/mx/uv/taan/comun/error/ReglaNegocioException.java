package mx.uv.taan.comun.error;

/**
 * Error de negocio con un código estable (el campo "codigo" del esquema Error del contrato)
 * y el estado HTTP con que debe responderse. No depende de Spring, para que el dominio
 * pueda probarse sin levantar el framework.
 */
public class ReglaNegocioException extends RuntimeException {

    private final int estadoHttp;
    private final String codigo;

    public ReglaNegocioException(int estadoHttp, String codigo, String mensaje) {
        super(mensaje);
        this.estadoHttp = estadoHttp;
        this.codigo = codigo;
    }

    public int getEstadoHttp() {
        return estadoHttp;
    }

    public String getCodigo() {
        return codigo;
    }
}
