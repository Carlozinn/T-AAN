package mx.uv.taan.comun.error;

/**
 * Límite preventivo excedido (RNF-009, RNF-022): responde 429 con código LIMITE_EXCEDIDO
 * y la cabecera Retry-After. El mensaje se redacta como medida preventiva y nunca revela
 * el valor del límite.
 */
public class LimiteExcedidoException extends ReglaNegocioException {

    private final long reintentarEnSegundos;

    public LimiteExcedidoException(long reintentarEnSegundos) {
        this(reintentarEnSegundos, "Por seguridad, espera unos minutos antes de volver a intentarlo.");
    }

    public LimiteExcedidoException(long reintentarEnSegundos, String mensaje) {
        super(429, "LIMITE_EXCEDIDO", mensaje);
        this.reintentarEnSegundos = reintentarEnSegundos;
    }

    public long getReintentarEnSegundos() {
        return reintentarEnSegundos;
    }
}
