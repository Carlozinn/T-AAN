package mx.uv.taan.reportes.api;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import mx.uv.taan.comun.api.Pagina;
import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.reportes.api.dto.DatosNuevoReporte;
import mx.uv.taan.reportes.api.dto.ReaperturaSolicitud;
import mx.uv.taan.reportes.aplicacion.NuevoReporte;
import mx.uv.taan.reportes.aplicacion.ReporteCreado;
import mx.uv.taan.reportes.aplicacion.ReporteDetalleCiudadano;
import mx.uv.taan.reportes.aplicacion.ReporteResumenCiudadano;
import mx.uv.taan.reportes.aplicacion.ServicioConsultaCiudadana;
import mx.uv.taan.reportes.aplicacion.ServicioRegistroReportes;
import mx.uv.taan.reportes.aplicacion.ServicioRespuestaCiudadana;
import tools.jackson.databind.ObjectMapper;

/**
 * Reportes del ciudadano (CU-01, CU-02, CU-03, CU-23; rol ciudadano).
 *
 * <p>«datos» se recibe como texto y se convierte aquí. Así funciona igual si el cliente lo manda
 * como application/json (la web y la app) o como texto simple (Swagger UI), que no permite
 * elegir el tipo de cada parte del multipart.</p>
 */
@RestController
@RequestMapping("/ciudadano/reportes")
public class ReportesCiudadanoController {

    private final ServicioRegistroReportes registro;
    private final ServicioConsultaCiudadana consulta;
    private final ServicioRespuestaCiudadana respuesta;
    private final ObjectMapper json;
    private final Validator validador;

    public ReportesCiudadanoController(ServicioRegistroReportes registro, ServicioConsultaCiudadana consulta,
                                       ServicioRespuestaCiudadana respuesta, ObjectMapper json, Validator validador) {
        this.registro = registro;
        this.consulta = consulta;
        this.respuesta = respuesta;
        this.json = json;
        this.validador = validador;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ReporteCreado crear(@RequestPart("datos") String datos,
                               @RequestPart(value = "fotos", required = false) List<MultipartFile> fotos,
                               @AuthenticationPrincipal Jwt sesion, HttpServletRequest peticion) {
        DatosNuevoReporte d = leerDatos(datos);
        NuevoReporte nuevo = new NuevoReporte(d.descripcion(), d.ubicacion().latitud(), d.ubicacion().longitud(),
                d.origen(), d.campoAuxiliar());
        return registro.registrar(idDe(sesion), nuevo, fotos, peticion.getRemoteAddr(), peticion.getHeader("User-Agent"));
    }

    @GetMapping
    public Pagina<ReporteResumenCiudadano> listar(@RequestParam(defaultValue = "0") int pagina,
                                                  @RequestParam(defaultValue = "20") int tamano,
                                                  @AuthenticationPrincipal Jwt sesion) {
        return consulta.listar(idDe(sesion), pagina, tamano);
    }

    @GetMapping("/{folio}")
    public ReporteDetalleCiudadano detalle(@PathVariable String folio, @AuthenticationPrincipal Jwt sesion) {
        return consulta.detalle(idDe(sesion), folio);
    }

    @PostMapping("/{folio}/reapertura")
    public ReporteDetalleCiudadano reabrir(@PathVariable String folio, @Valid @RequestBody ReaperturaSolicitud s,
                                           @AuthenticationPrincipal Jwt sesion) {
        return respuesta.reabrir(idDe(sesion), folio, s.motivo());
    }

    @PostMapping("/{folio}/confirmacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmar(@PathVariable String folio, @AuthenticationPrincipal Jwt sesion) {
        respuesta.confirmar(idDe(sesion), folio);
    }

    private DatosNuevoReporte leerDatos(String texto) {
        DatosNuevoReporte datos;
        try {
            datos = json.readValue(texto, DatosNuevoReporte.class);
        } catch (RuntimeException e) {
            throw new ReglaNegocioException(400, "VALIDACION", "Revisa el formato de los datos del reporte.");
        }
        Set<ConstraintViolation<DatosNuevoReporte>> errores = validador.validate(datos);
        if (!errores.isEmpty()) {
            ConstraintViolation<DatosNuevoReporte> primero = errores.iterator().next();
            throw new ReglaNegocioException(400, "VALIDACION",
                    "Revisa los datos: " + primero.getPropertyPath() + " " + primero.getMessage() + ".");
        }
        return datos;
    }

    private static long idDe(Jwt sesion) {
        return Long.parseLong(sesion.getSubject());
    }
}
