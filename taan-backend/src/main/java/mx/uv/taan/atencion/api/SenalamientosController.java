package mx.uv.taan.atencion.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mx.uv.taan.atencion.api.dto.ResolucionSenalamientoSolicitud;
import mx.uv.taan.atencion.aplicacion.SenalamientoVista;
import mx.uv.taan.atencion.aplicacion.ServicioSenalamientos;
import mx.uv.taan.atencion.dominio.EstadoSenalamiento;
import mx.uv.taan.comun.api.Pagina;
import mx.uv.taan.cuentas.aplicacion.UsuarioActual;

/** Revisión de señalamientos por el administrador (CU-26 pasos 2 y 3; rol administrador). */
@RestController
@RequestMapping("/admin/senalamientos")
public class SenalamientosController {

    private final ServicioSenalamientos senalamientos;

    public SenalamientosController(ServicioSenalamientos senalamientos) {
        this.senalamientos = senalamientos;
    }

    @GetMapping
    public Pagina<SenalamientoVista> listar(@RequestParam(defaultValue = "pendiente") EstadoSenalamiento estado,
                                            @RequestParam(defaultValue = "0") int pagina,
                                            @RequestParam(defaultValue = "20") int tamano) {
        return senalamientos.listar(estado, pagina, tamano);
    }

    @PostMapping("/{id}/resolucion")
    public SenalamientoVista resolver(@PathVariable long id, @Valid @RequestBody ResolucionSenalamientoSolicitud s,
                                      UsuarioActual usuario) {
        return senalamientos.resolver(usuario, id, s.decision() == ResolucionSenalamientoSolicitud.Decision.suspender,
                s.motivo(), s.diasSuspension());
    }
}
