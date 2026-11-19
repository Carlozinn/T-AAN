package mx.uv.taan.reportes.dominio;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Guarda {@link EstadoReporte} con su valor del tipo estado_reporte de PostgreSQL
 * (pendiente_canalizacion, recibido...). Las columnas que lo usan llevan además
 * {@code @ColumnTransformer(write = "?::estado_reporte")} para que PostgreSQL acepte el texto.
 */
@Converter
public class EstadoReporteConverter implements AttributeConverter<EstadoReporte, String> {

    @Override
    public String convertToDatabaseColumn(EstadoReporte estado) {
        return estado == null ? null : estado.valorBd();
    }

    @Override
    public EstadoReporte convertToEntityAttribute(String valor) {
        return valor == null ? null : EstadoReporte.desdeValorBd(valor);
    }
}
