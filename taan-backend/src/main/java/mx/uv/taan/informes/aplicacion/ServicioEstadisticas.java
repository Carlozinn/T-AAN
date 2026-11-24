package mx.uv.taan.informes.aplicacion;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uv.taan.administracion.aplicacion.OrganizacionVista;
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
import mx.uv.taan.reportes.aplicacion.ServicioExpedientes;

/**
 * Estadísticas e informes exportables (CU-10; RF-026, RF-041, RF-042).
 *
 * <p>Alcance (RF-026, punto 7): el personal de área solo obtiene los de su área (pedir otra
 * responde 403); la coordinación, los de toda su organización o de una de sus áreas (un área
 * ajena responde 404). El periodo abarca como máximo 366 días para acotar las consultas.</p>
 */
@Service
public class ServicioEstadisticas {

    static final int DIAS_MAXIMOS = 366;
    static final String TIPO_PDF = "application/pdf";
    static final String TIPO_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ServicioExpedientes expedientes;
    private final ServicioAreas areas;
    private final ServicioOrganizaciones organizaciones;
    private final InformeGeneradoRepository informes;
    private final GeneradorPdf pdf;
    private final GeneradorHojaCalculo hojaCalculo;

    public ServicioEstadisticas(ServicioExpedientes expedientes, ServicioAreas areas,
                                ServicioOrganizaciones organizaciones, InformeGeneradoRepository informes,
                                GeneradorPdf pdf, GeneradorHojaCalculo hojaCalculo) {
        this.expedientes = expedientes;
        this.areas = areas;
        this.organizaciones = organizaciones;
        this.informes = informes;
        this.pdf = pdf;
        this.hojaCalculo = hojaCalculo;
    }

    @Transactional(readOnly = true)
    public Estadisticas calcular(UsuarioActual usuario, LocalDate desde, LocalDate hasta, Long areaId) {
        validarPeriodo(desde, hasta);
        return estadisticas(usuario, desde, hasta, areaEnAlcance(usuario, areaId));
    }

    /** Genera el informe y registra quién lo generó y cuándo (RF-026, punto 9). Sin datos de contacto. */
    @Transactional
    public ArchivoInforme generarInforme(UsuarioActual usuario, LocalDate desde, LocalDate hasta, Long areaId,
                                         FormatoInforme formato) {
        validarPeriodo(desde, hasta);
        AreaVista area = areaEnAlcance(usuario, areaId);
        Estadisticas estadisticas = estadisticas(usuario, desde, hasta, area);
        InformeGenerado registro = informes.save(new InformeGenerado(usuario.id(), usuario.organizacionId(),
                area == null ? null : area.id(), desde, hasta, formato));
        String organizacion = organizaciones.consultar(usuario.organizacionId())
                .map(OrganizacionVista::nombre).orElse("");
        ContenidoInforme contenido = new ContenidoInforme(organizacion, area == null ? null : area.nombre(),
                estadisticas, registro.getCreadoEn());

        String nombre = "informe-taan-" + desde + "-a-" + hasta;
        return formato == FormatoInforme.pdf
                ? new ArchivoInforme(pdf.generar(contenido), TIPO_PDF, nombre + ".pdf")
                : new ArchivoInforme(hojaCalculo.generar(contenido), TIPO_XLSX, nombre + ".xlsx");
    }

    private Estadisticas estadisticas(UsuarioActual usuario, LocalDate desde, LocalDate hasta, AreaVista area) {
        IndicadoresReportes i = expedientes.indicadores(new FiltroIndicadores(usuario.organizacionId(),
                area == null ? null : area.id(), desde, hasta));
        return new Estadisticas(desde, hasta, i.porEstado(), i.porCategoria(), i.porArea(), i.porZona(),
                redondear(i.horasAsignacion()), redondear(i.horasResolucion()), i.enTiempo(), i.atrasados(),
                i.reabiertos(), i.redirigidos());
    }

    /** null = toda la organización (solo la coordinación). */
    private AreaVista areaEnAlcance(UsuarioActual usuario, Long areaId) {
        if (usuario.rol() == Rol.personal_area) {
            if (areaId != null && !areaId.equals(usuario.areaId())) {
                throw new ReglaNegocioException(403, "PROHIBIDO", "Solo puedes consultar las estadísticas de tu área.");
            }
            areaId = usuario.areaId();
        }
        if (areaId == null) {
            return null;
        }
        long buscada = areaId;
        return areas.listar(usuario.organizacionId()).stream().filter(a -> a.id() == buscada).findFirst()
                .orElseThrow(ReglaNegocioException::noEncontrado);
    }

    private static void validarPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new ReglaNegocioException(400, "VALIDACION", "Indica la fecha inicial y la final del periodo.");
        }
        if (hasta.isBefore(desde)) {
            throw new ReglaNegocioException(400, "VALIDACION", "La fecha final no puede ser anterior a la inicial.");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) >= DIAS_MAXIMOS) {
            throw new ReglaNegocioException(400, "VALIDACION",
                    "El periodo abarca como máximo " + DIAS_MAXIMOS + " días.");
        }
    }

    private static Double redondear(Double horas) {
        return horas == null ? null : Math.round(horas * 10) / 10.0;
    }
}
