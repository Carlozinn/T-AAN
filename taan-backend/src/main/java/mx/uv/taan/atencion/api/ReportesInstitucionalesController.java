package mx.uv.taan.atencion.api;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import mx.uv.taan.archivos.aplicacion.ArchivoRef;
import mx.uv.taan.atencion.api.dto.AsignacionSolicitud;
import mx.uv.taan.atencion.api.dto.MarcaDuplicadoSolicitud;
import mx.uv.taan.atencion.api.dto.RechazoSolicitud;
import mx.uv.taan.atencion.api.dto.ResolucionSolicitud;
import mx.uv.taan.atencion.api.dto.ValidacionSolicitud;
import mx.uv.taan.atencion.aplicacion.EventoHistorial;
import mx.uv.taan.atencion.aplicacion.ReporteDetalleInstitucional;
import mx.uv.taan.atencion.aplicacion.ReporteResumenInstitucional;
import mx.uv.taan.atencion.aplicacion.ServicioBandeja;
import mx.uv.taan.atencion.aplicacion.ServicioFlujoAtencion;
import mx.uv.taan.comun.api.Pagina;
import mx.uv.taan.cuentas.aplicacion.UsuarioActual;

/**
 * Atención institucional (CU-05 a CU-09, CU-17, CU-18; roles personal_area y coordinacion).
 * El alcance de cada usuario (su área o su organización) lo aplican los servicios.
 */
@RestController
@RequestMapping("/institucional/reportes")
public class ReportesInstitucionalesController {

    private final ServicioBandeja bandeja;
    private final ServicioFlujoAtencion flujo;

    public ReportesInstitucionalesController(ServicioBandeja bandeja, ServicioFlujoAtencion flujo) {
        this.bandeja = bandeja;
        this.flujo = flujo;
    }

    @GetMapping
    public Pagina<ReporteResumenInstitucional> buscar(
            @RequestParam(required = false) List<String> estado,
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String semaforo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String texto,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano,
            UsuarioActual usuario) {
        return bandeja.buscar(usuario, estado, areaId, categoriaId, semaforo, desde, hasta, texto, pagina, tamano);
    }

    @GetMapping("/{folio}")
    public ReporteDetalleInstitucional detalle(@PathVariable String folio, UsuarioActual usuario) {
        return bandeja.detalle(usuario, folio);
    }

    @GetMapping("/{folio}/historial")
    public List<EventoHistorial> historial(@PathVariable String folio, UsuarioActual usuario) {
        return bandeja.historial(usuario, folio);
    }

    @PostMapping("/{folio}/validacion")
    public ReporteDetalleInstitucional validar(@PathVariable String folio,
                                               @RequestBody(required = false) ValidacionSolicitud solicitud,
                                               UsuarioActual usuario) {
        return flujo.validar(usuario, folio, solicitud == null ? null : solicitud.categoriaFinalId());
    }

    @PostMapping("/{folio}/rechazo")
    public ReporteDetalleInstitucional rechazar(@PathVariable String folio, @Valid @RequestBody RechazoSolicitud s,
                                                UsuarioActual usuario) {
        return flujo.rechazar(usuario, folio, s.motivoPublico(), s.textoPublico(), s.justificacion());
    }

    @PostMapping("/{folio}/duplicado")
    public ReporteDetalleInstitucional marcarDuplicado(@PathVariable String folio,
                                                       @Valid @RequestBody MarcaDuplicadoSolicitud s,
                                                       UsuarioActual usuario) {
        return flujo.marcarDuplicado(usuario, folio, s.folioPrincipal(), s.observaciones());
    }

    @PostMapping("/{folio}/asignacion")
    public ReporteDetalleInstitucional asignar(@PathVariable String folio, @Valid @RequestBody AsignacionSolicitud s,
                                               UsuarioActual usuario) {
        return flujo.asignar(usuario, folio, s.areaId(), s.justificacion());
    }

    @PostMapping("/{folio}/inicio-atencion")
    public ReporteDetalleInstitucional iniciarAtencion(@PathVariable String folio, UsuarioActual usuario) {
        return flujo.iniciarAtencion(usuario, folio);
    }

    @PostMapping("/{folio}/resolucion")
    public ReporteDetalleInstitucional resolver(@PathVariable String folio, @Valid @RequestBody ResolucionSolicitud s,
                                                UsuarioActual usuario) {
        return flujo.resolver(usuario, folio, s.observacionCierre());
    }

    @PostMapping(path = "/{folio}/evidencias", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ArchivoRef adjuntarEvidencia(@PathVariable String folio, @RequestPart("archivo") MultipartFile archivo,
                                        UsuarioActual usuario) {
        return flujo.adjuntarEvidencia(usuario, folio, archivo);
    }
}
