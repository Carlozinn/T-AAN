package mx.uv.taan.reportes.aplicacion;

import java.time.Instant;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uv.taan.administracion.aplicacion.ServicioOrganizaciones;
import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.cuentas.aplicacion.ServicioPerfilCiudadano;
import mx.uv.taan.reportes.dominio.EstadoReporte;
import mx.uv.taan.reportes.dominio.HistorialReporte;
import mx.uv.taan.reportes.dominio.Reporte;
import mx.uv.taan.reportes.infraestructura.HistorialReporteRepository;
import mx.uv.taan.reportes.infraestructura.ReporteRepository;

/**
 * Respuesta del ciudadano a «¿Se resolvió tu problema?» (CU-23; RF-042): reabrir el reporte o
 * confirmar la resolución. Una cuenta suspendida no puede hacer ninguna de las dos (EX-02), y
 * después de confirmar ya no se puede reabrir.
 */
@Service
public class ServicioRespuestaCiudadana {

    private final ReporteRepository reportes;
    private final HistorialReporteRepository historial;
    private final ServicioConsultaCiudadana consulta;
    private final ServicioOrganizaciones organizaciones;
    private final ServicioPerfilCiudadano perfiles;

    public ServicioRespuestaCiudadana(ReporteRepository reportes, HistorialReporteRepository historial,
                                      ServicioConsultaCiudadana consulta, ServicioOrganizaciones organizaciones,
                                      ServicioPerfilCiudadano perfiles) {
        this.reportes = reportes;
        this.historial = historial;
        this.consulta = consulta;
        this.organizaciones = organizaciones;
        this.perfiles = perfiles;
    }

    @Transactional
    public ReporteDetalleCiudadano reabrir(long ciudadanoId, String folio, String motivo) {
        Reporte r = bloquearPropio(ciudadanoId, folio);
        if (r.getEstado() == EstadoReporte.RESUELTO && consulta.confirmado(r.getId())) {
            throw ReglaNegocioException.conflicto("YA_CONFIRMADO",
                    "Confirmaste que el problema se resolvió. Si volvió a ocurrir, registra un reporte nuevo.");
        }
        r.reabrir(consulta.plazoReapertura(organizaciones.consultar(r.getOrganizacionId())), Instant.now());
        historial.save(HistorialReporte.reapertura(r.getId(), motivo.strip()));
        return consulta.detalle(ciudadanoId, folio);
    }

    @Transactional
    public void confirmar(long ciudadanoId, String folio) {
        Reporte r = bloquearPropio(ciudadanoId, folio);
        if (r.getEstado() != EstadoReporte.RESUELTO) {
            throw ReglaNegocioException.conflicto("TRANSICION_INVALIDA", "Solo se confirma un reporte Resuelto.");
        }
        if (consulta.confirmado(r.getId())) {
            throw ReglaNegocioException.conflicto("YA_CONFIRMADO", "Ya confirmaste que el problema se resolvió.");
        }
        historial.save(HistorialReporte.confirmacion(r.getId()));
    }

    /** Un folio de otra cuenta responde 404, sin revelar que existe. */
    private Reporte bloquearPropio(long ciudadanoId, String folio) {
        if (perfiles.condicionParaReportar(ciudadanoId).suspendida()) {
            throw new ReglaNegocioException(403, "CUENTA_SUSPENDIDA",
                    "Tu cuenta está suspendida: puedes consultar tus reportes, pero no responder a su resolución.");
        }
        return reportes.bloquearPorFolio(folio)
                .filter(r -> Objects.equals(r.getCiudadanoId(), ciudadanoId))
                .orElseThrow(ReglaNegocioException::noEncontrado);
    }
}
