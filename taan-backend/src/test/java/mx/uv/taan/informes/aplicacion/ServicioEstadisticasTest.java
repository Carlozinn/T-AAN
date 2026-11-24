package mx.uv.taan.informes.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

import mx.uv.taan.administracion.aplicacion.ServicioOrganizaciones;
import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.configuracion.aplicacion.AreaVista;
import mx.uv.taan.configuracion.aplicacion.ServicioAreas;
import mx.uv.taan.cuentas.aplicacion.UsuarioActual;
import mx.uv.taan.cuentas.dominio.Rol;
import mx.uv.taan.informes.dominio.FormatoInforme;
import mx.uv.taan.informes.dominio.InformeGenerado;
import mx.uv.taan.informes.infraestructura.GeneradorHojaCalculo;
import mx.uv.taan.informes.infraestructura.GeneradorPdf;
import mx.uv.taan.informes.infraestructura.InformeGeneradoRepository;
import mx.uv.taan.reportes.aplicacion.FiltroIndicadores;
import mx.uv.taan.reportes.aplicacion.IndicadoresReportes;
import mx.uv.taan.reportes.aplicacion.IndicadoresReportes.Conteo;
import mx.uv.taan.reportes.aplicacion.ServicioExpedientes;

class ServicioEstadisticasTest {

    private static final long ORG = 1L;
    private static final long OBRAS = 4L;
    private static final LocalDate DESDE = LocalDate.of(2026, 1, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 3, 31);

    private ServicioExpedientes expedientes;
    private InformeGeneradoRepository informes;
    private ServicioEstadisticas servicio;

    @TempDir
    Path carpeta;

    private final UsuarioActual coordinacion = new UsuarioActual(2L, Rol.coordinacion, ORG, null);
    private final UsuarioActual personalObras = new UsuarioActual(3L, Rol.personal_area, ORG, OBRAS);

    @BeforeEach
    void preparar() {
        expedientes = mock(ServicioExpedientes.class);
        informes = mock(InformeGeneradoRepository.class);
        ServicioAreas areas = mock(ServicioAreas.class);
        servicio = new ServicioEstadisticas(expedientes, areas, mock(ServicioOrganizaciones.class), informes,
                new GeneradorPdf(), new GeneradorHojaCalculo());
        when(areas.listar(ORG)).thenReturn(List.of(new AreaVista(OBRAS, "Obras Públicas", null, null, null, true)));
        when(expedientes.indicadores(any())).thenReturn(new IndicadoresReportes(
                List.of(new Conteo("en_atencion", 5), new Conteo("resuelto", 3)),
                List.of(new Conteo("Baches", 8)), List.of(new Conteo("Obras Públicas", 8)),
                List.of(new Conteo("Cabecera municipal", 8)), 12.345, 70.0, 4, 1, 2, 3));
        when(informes.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void laCoordinacionObtieneLosDeTodaSuOrganizacion() {
        Estadisticas e = servicio.calcular(coordinacion, DESDE, HASTA, null);

        assertEquals(new FiltroIndicadores(ORG, null, DESDE, HASTA), filtroUsado());
        assertEquals(12.3, e.tiempoPromedioAsignacionHoras());
        assertEquals(3, e.redirigidos());
        assertEquals(DESDE, e.periodoInicio());
    }

    @Test
    void elPersonalDeAreaSoloObtieneLosDeSuArea() {
        servicio.calcular(personalObras, DESDE, HASTA, null);

        assertEquals(OBRAS, filtroUsado().areaId());
    }

    @Test
    void elPersonalDeAreaNoPuedePedirOtraArea() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.calcular(personalObras, DESDE, HASTA, 1L));

        assertEquals(403, error.getEstadoHttp());
        verify(expedientes, never()).indicadores(any());
    }

    @Test
    void unAreaDeOtraOrganizacionResponde404() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.calcular(coordinacion, DESDE, HASTA, 77L));

        assertEquals(404, error.getEstadoHttp());
    }

    @Test
    void elPeriodoNoPuedeSuperar366Dias() {
        servicio.calcular(coordinacion, DESDE, DESDE.plusDays(365), null);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.calcular(coordinacion, DESDE, DESDE.plusDays(366), null));
        assertEquals(400, error.getEstadoHttp());
    }

    @Test
    void laFechaFinalNoPuedeSerAnteriorALaInicial() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.calcular(coordinacion, HASTA, DESDE, null));

        assertEquals(400, error.getEstadoHttp());
    }

    @Test
    void elInformePdfRegistraQuienLoGeneroYContieneLosIndicadores() throws IOException {
        ArchivoInforme archivo = servicio.generarInforme(personalObras, DESDE, HASTA, null, FormatoInforme.pdf);

        ArgumentCaptor<InformeGenerado> registro = ArgumentCaptor.forClass(InformeGenerado.class);
        verify(informes).save(registro.capture());
        assertEquals(3L, registro.getValue().getUsuarioId());
        assertEquals(OBRAS, registro.getValue().getAreaId());
        assertEquals(FormatoInforme.pdf, registro.getValue().getFormato());

        assertEquals("application/pdf", archivo.tipoContenido());
        assertEquals("informe-taan-2026-01-01-a-2026-03-31.pdf", archivo.nombreArchivo());
        String texto = new PdfTextExtractor(new PdfReader(archivo.contenido())).getTextFromPage(1);
        assertTrue(texto.contains("Obras Públicas"), texto);
        assertTrue(texto.contains("En atención"), texto);
        assertTrue(texto.contains("12.3"), texto);
    }

    @Test
    void elInformeEnHojaDeCalculoEsUnXlsxConLosMismosDatos() throws IOException {
        ArchivoInforme archivo = servicio.generarInforme(coordinacion, DESDE, HASTA, null, FormatoInforme.hoja_calculo);

        assertEquals(ServicioEstadisticas.TIPO_XLSX, archivo.tipoContenido());
        assertTrue(archivo.nombreArchivo().endsWith(".xlsx"));
        String contenido = textoDelXlsx(archivo.contenido());
        assertTrue(contenido.contains("Toda la organización"));
        assertTrue(contenido.contains("Cabecera municipal"));
        assertTrue(contenido.contains("Reportes redirigidos a otra organización"));
    }

    @Test
    void sinDatosLosTiemposPromedioQuedanNulos() {
        when(expedientes.indicadores(any())).thenReturn(new IndicadoresReportes(List.of(), List.of(), List.of(),
                List.of(), null, null, 0, 0, 0, 0));

        Estadisticas e = servicio.calcular(coordinacion, DESDE, HASTA, null);

        assertNull(e.tiempoPromedioResolucionHoras());
        assertTrue(e.porEstado().isEmpty());
    }

    private FiltroIndicadores filtroUsado() {
        ArgumentCaptor<FiltroIndicadores> filtro = ArgumentCaptor.forClass(FiltroIndicadores.class);
        verify(expedientes).indicadores(filtro.capture());
        return filtro.getValue();
    }

    /**
     * Une el XML de todas las partes del .xlsx, con las entidades numéricas (&#xf3; = ó)
     * decodificadas: basta para comprobar que los textos están.
     */
    private String textoDelXlsx(byte[] xlsx) throws IOException {
        Path archivo = carpeta.resolve("informe.xlsx");
        Files.write(archivo, xlsx);
        StringBuilder texto = new StringBuilder();
        try (ZipFile zip = new ZipFile(archivo.toFile())) {
            for (ZipEntry parte : Collections.list(zip.entries())) {
                texto.append(new String(zip.getInputStream(parte).readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return Pattern.compile("&#x([0-9a-fA-F]+);").matcher(texto)
                .replaceAll(m -> Character.toString(Integer.parseInt(m.group(1), 16)));
    }
}
