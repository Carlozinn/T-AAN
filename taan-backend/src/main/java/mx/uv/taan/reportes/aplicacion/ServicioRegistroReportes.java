package mx.uv.taan.reportes.aplicacion;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import mx.uv.taan.administracion.aplicacion.ServicioZonas;
import mx.uv.taan.archivos.aplicacion.ServicioArchivos;
import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.cuentas.aplicacion.CondicionCiudadano;
import mx.uv.taan.cuentas.aplicacion.ServicioPerfilCiudadano;
import mx.uv.taan.reportes.dominio.HistorialReporte;
import mx.uv.taan.reportes.dominio.Reporte;
import mx.uv.taan.reportes.infraestructura.HistorialReporteRepository;
import mx.uv.taan.reportes.infraestructura.RegistroEnvios;
import mx.uv.taan.reportes.infraestructura.ReporteRepository;
import mx.uv.taan.reportes.infraestructura.SecuenciaFolios;

/**
 * Registro de un reporte ciudadano (CU-01; RF-004 a RF-006, RF-011, RF-038, RNF-022).
 *
 * <p>Responde de inmediato con el folio. La clasificación con IA y la canalización por reglas
 * ocurren después, en ProcesadorReportes, cuando esta transacción ya se confirmó (RNF-011).
 * Los mensajes de rechazo se redactan como medidas preventivas, nunca como fallas.</p>
 */
@Service
public class ServicioRegistroReportes {

    static final int MAX_FOTOS = 3;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("America/Mexico_City"));

    private final ReporteRepository reportes;
    private final HistorialReporteRepository historial;
    private final SecuenciaFolios folios;
    private final RegistroEnvios envios;
    private final ServicioPerfilCiudadano perfiles;
    private final ServicioZonas zonas;
    private final ServicioArchivos archivos;
    private final ApplicationEventPublisher eventos;

    public ServicioRegistroReportes(ReporteRepository reportes, HistorialReporteRepository historial,
                                    SecuenciaFolios folios, RegistroEnvios envios, ServicioPerfilCiudadano perfiles,
                                    ServicioZonas zonas, ServicioArchivos archivos, ApplicationEventPublisher eventos) {
        this.reportes = reportes;
        this.historial = historial;
        this.folios = folios;
        this.envios = envios;
        this.perfiles = perfiles;
        this.zonas = zonas;
        this.archivos = archivos;
        this.eventos = eventos;
    }

    @Transactional
    public ReporteCreado registrar(long ciudadanoId, NuevoReporte datos, List<MultipartFile> fotos,
                                   String origenIp, String agente) {
        // 1. Campo trampa: solo un bot lo llena (RNF-022).
        if (datos.campoAuxiliar() != null && !datos.campoAuxiliar().isEmpty()) {
            envios.registrarRechazo(origenIp, ciudadanoId, agente, "rechazado_honeypot");
            throw new ReglaNegocioException(400, "VALIDACION", "Revisa los datos del formulario.");
        }
        // 2. La cuenta: correo verificado, sin suspensión vigente y con teléfono (EX-07, EX-08).
        CondicionCiudadano cuenta = perfiles.condicionParaReportar(ciudadanoId);
        if (!cuenta.correoVerificado()) {
            envios.registrarRechazo(origenIp, ciudadanoId, agente, "rechazado_verificacion");
            throw new ReglaNegocioException(403, "CORREO_NO_VERIFICADO",
                    "Confirma tu correo con el enlace que te enviamos antes de enviar reportes.");
        }
        if (cuenta.suspendida()) {
            envios.registrarRechazo(origenIp, ciudadanoId, agente, "rechazado_suspension");
            throw new ReglaNegocioException(403, "CUENTA_SUSPENDIDA", mensajeSuspension(cuenta));
        }
        if (cuenta.requiereTelefono()) {
            throw ReglaNegocioException.noProcesable("TELEFONO_REQUERIDO",
                    "Agrega tu teléfono en tu perfil antes de enviar tu primer reporte.");
        }
        // 3. Número de fotografías (RNF-022, punto 4); cada una se valida al guardarla.
        List<MultipartFile> adjuntas = fotos == null ? List.of()
                : fotos.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (adjuntas.size() > MAX_FOTOS) {
            throw new ReglaNegocioException(400, "VALIDACION", "Puedes adjuntar hasta " + MAX_FOTOS + " fotografías.");
        }
        // 4. Límite de envíos por cuenta y por origen (EX-06).
        envios.asegurarDentroDelLimite(origenIp, ciudadanoId, agente);
        // 5. Cobertura: fuera de toda zona el reporte no se registra (EX-05, RF-037 punto 4).
        if (!zonas.cobertura(datos.latitud(), datos.longitud()).dentroDeCobertura()) {
            throw ReglaNegocioException.noProcesable("FUERA_DE_COBERTURA",
                    "T'AAN aún no opera en esa ubicación. Si es una emergencia, llama al 911.");
        }
        // 6. Guardar: reporte, evento inicial, fotografías válidas y registro técnico del envío.
        Reporte reporte = reportes.save(new Reporte(folios.siguiente(), ciudadanoId, datos.descripcion().strip(),
                datos.latitud(), datos.longitud(), datos.origen()));
        historial.save(HistorialReporte.registro(reporte.getId()));
        archivos.guardarFotosCiudadanas(reporte.getId(), adjuntas);
        envios.registrarAceptado(origenIp, ciudadanoId, agente, reporte.getId());
        eventos.publishEvent(new ReporteRegistrado(reporte.getId()));

        return new ReporteCreado(reporte.getFolio(), reporte.getEstado().valorBd(),
                reporte.getEstado().textoCiudadano(), reporte.getCreadoEn());
    }

    private static String mensajeSuspension(CondicionCiudadano cuenta) {
        String vigencia = cuenta.suspendidaHasta() == null
                ? "por tiempo indefinido"
                : "hasta el " + FECHA.format(cuenta.suspendidaHasta());
        return "Tu cuenta está suspendida " + vigencia + ": " + cuenta.motivoSuspension()
                + ". Puedes seguir consultando tus reportes.";
    }
}
