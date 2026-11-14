package mx.uv.taan.comun.error;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Convierte las excepciones en respuestas con el esquema Error del contrato.
 * Los mensajes al ciudadano se redactan como medidas preventivas (RNF-022), nunca como fallas.
 */
@RestControllerAdvice
public class ManejadorErrores {

    /** 429 con Retry-After (RNF-009, RNF-022); más específico que ReglaNegocioException. */
    @ExceptionHandler(LimiteExcedidoException.class)
    public ResponseEntity<ApiError> limiteExcedido(LimiteExcedidoException e) {
        return ResponseEntity.status(e.getEstadoHttp())
                .header("Retry-After", Long.toString(e.getReintentarEnSegundos()))
                .body(new ApiError(e.getCodigo(), e.getMessage(), List.of()));
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ApiError> reglaNegocio(ReglaNegocioException e) {
        return ResponseEntity.status(e.getEstadoHttp())
                .body(new ApiError(e.getCodigo(), e.getMessage(), List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validacion(MethodArgumentNotValidException e) {
        List<ApiError.DetalleCampo> detalles = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new ApiError.DetalleCampo(f.getField(), f.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(new ApiError("VALIDACION", "Revisa los datos marcados.", detalles));
    }

    /** Cuerpo ausente o JSON malformado: también es un 400 de validación. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> cuerpoIlegible(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(new ApiError("VALIDACION", "Revisa el formato de los datos enviados.", List.of()));
    }

    /** Que el 403 de la seguridad por métodos no se convierta en 500. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> accesoDenegado(AccessDeniedException e) {
        return ResponseEntity.status(403)
                .body(new ApiError("PROHIBIDO", "Tu cuenta no puede realizar esta operación.", List.of()));
    }
}
