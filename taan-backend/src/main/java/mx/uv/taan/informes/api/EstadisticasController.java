package mx.uv.taan.informes.api;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mx.uv.taan.cuentas.aplicacion.UsuarioActual;
import mx.uv.taan.informes.api.dto.InformeSolicitud;
import mx.uv.taan.informes.aplicacion.ArchivoInforme;
import mx.uv.taan.informes.aplicacion.Estadisticas;
import mx.uv.taan.informes.aplicacion.ServicioEstadisticas;

/** Estadísticas e informes (CU-10; roles personal_area y coordinacion). El alcance lo aplica el servicio. */
@RestController
@RequestMapping("/institucional")
public class EstadisticasController {

    private final ServicioEstadisticas estadisticas;

    public EstadisticasController(ServicioEstadisticas estadisticas) {
        this.estadisticas = estadisticas;
    }

    @GetMapping("/estadisticas")
    public Estadisticas obtener(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                @RequestParam(required = false) Long areaId, UsuarioActual usuario) {
        return estadisticas.calcular(usuario, desde, hasta, areaId);
    }

    @PostMapping("/informes")
    public ResponseEntity<byte[]> generar(@Valid @RequestBody InformeSolicitud s, UsuarioActual usuario) {
        ArchivoInforme archivo = estadisticas.generarInforme(usuario, s.periodoInicio(), s.periodoFin(), s.areaId(),
                s.formato());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.tipoContenido()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo.nombreArchivo()).build().toString())
                .body(archivo.contenido());
    }
}
