package mx.uv.taan.configuracion.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mx.uv.taan.administracion.aplicacion.CorrespondenciaVista;
import mx.uv.taan.administracion.aplicacion.ServicioCorrespondencia;
import mx.uv.taan.configuracion.api.dto.ConfirmacionCorrespondenciaSolicitud;
import mx.uv.taan.cuentas.aplicacion.UsuarioActual;

/**
 * Correspondencia categoría → área de la organización (CU-22; rol coordinacion). Las reglas
 * viven en administración; aquí la coordinación solo decide el área destino de las suyas.
 */
@RestController
@RequestMapping("/organizacion/correspondencia")
public class CorrespondenciaController {

    private final ServicioCorrespondencia correspondencia;

    public CorrespondenciaController(ServicioCorrespondencia correspondencia) {
        this.correspondencia = correspondencia;
    }

    @GetMapping
    public List<CorrespondenciaVista> consultar(UsuarioActual coordinacion) {
        return correspondencia.consultar(coordinacion.organizacionId());
    }

    @PutMapping
    public List<CorrespondenciaVista> confirmar(@Valid @RequestBody ConfirmacionCorrespondenciaSolicitud solicitud,
                                                UsuarioActual coordinacion) {
        List<ServicioCorrespondencia.Elemento> elementos = solicitud.elementos().stream()
                .map(e -> new ServicioCorrespondencia.Elemento(e.categoriaId(), e.areaId()))
                .toList();
        return correspondencia.confirmar(coordinacion.organizacionId(), elementos);
    }
}
