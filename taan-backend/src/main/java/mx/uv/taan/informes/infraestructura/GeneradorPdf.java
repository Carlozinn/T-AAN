package mx.uv.taan.informes.infraestructura;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import mx.uv.taan.informes.aplicacion.ContenidoInforme;
import mx.uv.taan.reportes.aplicacion.IndicadoresReportes.Conteo;

/** Informe en PDF (RF-026, punto 6) con OpenPDF: encabezado, indicadores y una tabla por sección. */
@Component
public class GeneradorPdf {

    private static final Font TITULO = new Font(Font.HELVETICA, 16, Font.BOLD);
    private static final Font SUBTITULO = new Font(Font.HELVETICA, 12, Font.BOLD);
    private static final Font NORMAL = new Font(Font.HELVETICA, 10);
    private static final Font NEGRITA = new Font(Font.HELVETICA, 10, Font.BOLD);

    public byte[] generar(ContenidoInforme contenido) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.LETTER, 50, 50, 50, 50);
        PdfWriter.getInstance(documento, salida);
        documento.open();
        documento.add(new Paragraph(contenido.titulo(), TITULO));
        documento.add(tabla(contenido.encabezado()));
        documento.add(new Paragraph("Indicadores", SUBTITULO));
        documento.add(tabla(contenido.indicadores()));
        for (ContenidoInforme.Seccion seccion : contenido.secciones()) {
            documento.add(new Paragraph(seccion.titulo(), SUBTITULO));
            if (seccion.conteos().isEmpty()) {
                documento.add(new Paragraph("Sin reportes en el periodo.", NORMAL));
                continue;
            }
            PdfPTable tabla = nuevaTabla();
            for (Conteo c : seccion.conteos()) {
                tabla.addCell(celda(c.etiqueta(), NORMAL, Element.ALIGN_LEFT));
                tabla.addCell(celda(Long.toString(c.total()), NORMAL, Element.ALIGN_RIGHT));
            }
            documento.add(tabla);
        }
        documento.close();
        return salida.toByteArray();
    }

    private static PdfPTable tabla(List<ContenidoInforme.Fila> filas) {
        PdfPTable tabla = nuevaTabla();
        for (ContenidoInforme.Fila fila : filas) {
            tabla.addCell(celda(fila.etiqueta(), NEGRITA, Element.ALIGN_LEFT));
            tabla.addCell(celda(fila.valor(), NORMAL, Element.ALIGN_RIGHT));
        }
        return tabla;
    }

    private static PdfPTable nuevaTabla() {
        PdfPTable tabla = new PdfPTable(new float[] {3, 1});
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(6);
        tabla.setSpacingAfter(12);
        return tabla;
    }

    private static PdfPCell celda(String texto, Font fuente, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, fuente));
        celda.setHorizontalAlignment(alineacion);
        celda.setPadding(4);
        return celda;
    }
}
