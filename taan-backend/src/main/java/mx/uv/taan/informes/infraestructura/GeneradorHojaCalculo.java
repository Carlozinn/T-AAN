package mx.uv.taan.informes.infraestructura;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.springframework.stereotype.Component;

import mx.uv.taan.informes.aplicacion.ContenidoInforme;
import mx.uv.taan.reportes.aplicacion.IndicadoresReportes.Conteo;

/** Informe en hoja de cálculo .xlsx (RF-026, punto 6) con fastexcel: una sola hoja, mismo contenido que el PDF. */
@Component
public class GeneradorHojaCalculo {

    public byte[] generar(ContenidoInforme contenido) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            Workbook libro = new Workbook(salida, "TAAN", "1.0");
            Worksheet hoja = libro.newWorksheet("Informe");
            hoja.width(0, 45);
            hoja.width(1, 15);
            int fila = 0;
            hoja.value(fila, 0, contenido.titulo());
            hoja.style(fila, 0).bold().fontSize(14).set();
            fila = filas(hoja, fila + 2, contenido.encabezado());
            hoja.value(fila, 0, "Indicadores");
            hoja.style(fila, 0).bold().set();
            fila = filas(hoja, fila + 1, contenido.indicadores());
            for (ContenidoInforme.Seccion seccion : contenido.secciones()) {
                hoja.value(fila, 0, seccion.titulo());
                hoja.value(fila, 1, "Reportes");
                hoja.range(fila, 0, fila, 1).style().bold().set();
                fila++;
                for (Conteo c : seccion.conteos()) {
                    hoja.value(fila, 0, c.etiqueta());
                    hoja.value(fila, 1, c.total());
                    fila++;
                }
                fila++;
            }
            libro.finish();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return salida.toByteArray();
    }

    /** Escribe pares etiqueta-valor y deja una fila en blanco; devuelve la siguiente fila libre. */
    private static int filas(Worksheet hoja, int desde, List<ContenidoInforme.Fila> filas) {
        int fila = desde;
        for (ContenidoInforme.Fila f : filas) {
            hoja.value(fila, 0, f.etiqueta());
            hoja.value(fila, 1, f.valor());
            fila++;
        }
        return fila + 1;
    }
}
