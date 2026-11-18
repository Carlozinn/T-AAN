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

    /** 404: no existe o no pertenece al ámbito del usuario; no se revela cuál de los dos (contrato). */
    public static ReglaNegocioException noEncontrado() {
        return new ReglaNegocioException(404, "NO_ENCONTRADO", "No encontramos lo que buscas.");
    }

    /** 409: conflicto con el estado actual (nombre o correo repetido, plantilla ya aplicada...). */
    public static ReglaNegocioException conflicto(String codigo, String mensaje) {
        return new ReglaNegocioException(409, codigo, mensaje);
    }

    /** 422: regla de negocio no cumplida. */
    public static ReglaNegocioException noProcesable(String codigo, String mensaje) {
        return new ReglaNegocioException(422, codigo, mensaje);
    }

    public int getEstadoHttp() {
        return estadoHttp;
    }

    public String getCodigo() {
        return codigo;
    }
}
