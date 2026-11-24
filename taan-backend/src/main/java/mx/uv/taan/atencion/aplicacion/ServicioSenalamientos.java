package mx.uv.taan.atencion.aplicacion;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uv.taan.administracion.aplicacion.ServicioOrganizaciones;
import mx.uv.taan.atencion.dominio.EstadoSenalamiento;
import mx.uv.taan.atencion.dominio.Senalamiento;
import mx.uv.taan.atencion.infraestructura.SenalamientoRepository;
import mx.uv.taan.comun.api.Pagina;
import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.cuentas.aplicacion.ServicioCuentasInstitucionales;
import mx.uv.taan.cuentas.aplicacion.ServicioSuspensiones;
import mx.uv.taan.cuentas.aplicacion.UsuarioActual;
import mx.uv.taan.reportes.aplicacion.ServicioExpedientes;
import mx.uv.taan.reportes.dominio.Reporte;

/**
 * Señalamiento y suspensión de cuentas (CU-26; RF-043).
 *
 * <p>El personal señala un reporte de su alcance; el estado del reporte no cambia. El
 * administrador desestima o suspende: la primera suspensión de una cuenta siempre tiene
 * vigencia y solo la reincidencia admite una indefinida. La suspensión recae en la cuenta y
 * no toca los reportes.</p>
 */
@Service
public class ServicioSenalamientos {

    private static final int TAMANO_MAXIMO = 100;

    private final SenalamientoRepository senalamientos;
    private final ServicioExpedientes expedientes;
    private final ServicioSuspensiones suspensiones;
    private final ServicioCuentasInstitucionales cuentas;
    private final ServicioOrganizaciones organizaciones;

    public ServicioSenalamientos(SenalamientoRepository senalamientos, ServicioExpedientes expedientes,
                                 ServicioSuspensiones suspensiones, ServicioCuentasInstitucionales cuentas,
                                 ServicioOrganizaciones organizaciones) {
        this.senalamientos = senalamientos;
        this.expedientes = expedientes;
        this.suspensiones = suspensiones;
        this.cuentas = cuentas;
        this.organizaciones = organizaciones;
    }

    /** CU-26 paso 1. Bloquea el reporte para que dos señalamientos simultáneos no se dupliquen. */
    @Transactional
    public void senalar(UsuarioActual usuario, String folio, String motivo) {
        Reporte reporte = expedientes.bloquearPorFolio(folio);
        if (!ServicioBandeja.estaEnAlcance(usuario, reporte)) {
            throw ReglaNegocioException.noEncontrado();
        }
        if (reporte.getCiudadanoId() == null) {
            throw cuentaEliminada();
        }
        if (senalamientos.existsByReporteIdAndEstado(reporte.getId(), EstadoSenalamiento.pendiente)) {
            throw ReglaNegocioException.conflicto("SENALAMIENTO_PENDIENTE",
                    "Este reporte ya tiene un señalamiento en revisión.");
        }
        senalamientos.save(new Senalamiento(reporte.getId(), reporte.getCiudadanoId(), usuario.id(), motivo.strip()));
    }

    /** CU-26 paso 2: la cola del administrador, con los antecedentes de cada cuenta. */
    @Transactional(readOnly = true)
    public Pagina<SenalamientoVista> listar(EstadoSenalamiento estado, int pagina, int tamano) {
        Page<Senalamiento> resultado = senalamientos.findByEstadoOrderByCreadoEnAscIdAsc(estado,
                PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamano, 1), TAMANO_MAXIMO)));
        Contexto contexto = contexto(resultado.getContent());
        return Pagina.de(resultado, s -> vista(s, contexto));
    }

    /**
     * CU-26 paso 3. Resolver primero hace que un señalamiento ya resuelto se rechace (409) antes
     * de tocar la cuenta; cualquier error posterior deshace también la resolución.
     */
    @Transactional
    public SenalamientoVista resolver(UsuarioActual administrador, long id, boolean suspender, String motivo,
                                      Integer diasSuspension) {
        Senalamiento senalamiento = senalamientos.bloquear(id).orElseThrow(ReglaNegocioException::noEncontrado);
        String texto = motivo.strip();
        if (suspender) {
            long suspensionesPrevias = senalamiento.getCiudadanoId() == null ? 0
                    : senalamientos.countByCiudadanoIdAndEstado(senalamiento.getCiudadanoId(), EstadoSenalamiento.aceptado);
            senalamiento.aceptar(administrador.id(), texto);
            suspenderCuenta(senalamiento.getCiudadanoId(), texto, diasSuspension, suspensionesPrevias);
        } else {
            senalamiento.desestimar(administrador.id(), texto);
        }
        return vista(senalamiento, contexto(List.of(senalamiento)));
    }

    private void suspenderCuenta(Long ciudadanoId, String motivo, Integer dias, long suspensionesPrevias) {
        if (ciudadanoId == null) {
            throw cuentaEliminada();
        }
        if (dias == null && suspensionesPrevias == 0) {
            throw new ReglaNegocioException(400, "VALIDACION",
                    "La primera suspensión de una cuenta debe tener vigencia (diasSuspension).");
        }
        Instant hasta = dias == null ? null : Instant.now().plus(Duration.ofDays(dias));
        if (!suspensiones.suspender(ciudadanoId, motivo, hasta)) {
            throw cuentaEliminada();
        }
    }

    private static ReglaNegocioException cuentaEliminada() {
        return ReglaNegocioException.conflicto("CUENTA_ELIMINADA", "La cuenta de este reporte ya fue eliminada.");
    }

    // ------------------------------------------------------------------ armado

    /** Datos de toda una página reunidos en lote: una consulta por tipo de dato, sin N+1. */
    private record Contexto(Map<Long, Reporte> reportes, Map<Long, String> organizaciones, Map<Long, String> personal,
                            Map<Long, String> ciudadanos, Map<Long, long[]> antecedentes) {
    }

    private Contexto contexto(List<Senalamiento> lista) {
        Map<Long, Reporte> reportes = expedientes.porIds(lista.stream().map(Senalamiento::getReporteId).toList());
        Set<Long> ciudadanoIds = lista.stream().map(Senalamiento::getCiudadanoId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, long[]> antecedentes = new HashMap<>();
        if (!ciudadanoIds.isEmpty()) {
            for (Object[] fila : senalamientos.antecedentes(ciudadanoIds, EstadoSenalamiento.aceptado)) {
                antecedentes.put(((Number) fila[0]).longValue(),
                        new long[] {((Number) fila[1]).longValue(), ((Number) fila[2]).longValue()});
            }
        }
        return new Contexto(reportes,
                organizaciones.nombres(reportes.values().stream().map(Reporte::getOrganizacionId)
                        .filter(Objects::nonNull).collect(Collectors.toSet())),
                cuentas.nombres(lista.stream().map(Senalamiento::getSenaladoPor).collect(Collectors.toSet())),
                ciudadanoIds.isEmpty() ? Map.of() : suspensiones.nombres(ciudadanoIds), antecedentes);
    }

    private static SenalamientoVista vista(Senalamiento s, Contexto c) {
        Reporte reporte = c.reportes().get(s.getReporteId());
        long[] previos = c.antecedentes().getOrDefault(s.getCiudadanoId(), new long[] {1, 0});
        int aceptadoPropio = s.getEstado() == EstadoSenalamiento.aceptado ? 1 : 0;
        EventoHistorial.UsuarioRef ciudadano = s.getCiudadanoId() == null ? null
                : new EventoHistorial.UsuarioRef(s.getCiudadanoId(), c.ciudadanos().get(s.getCiudadanoId()));
        return new SenalamientoVista(s.getId(), reporte.getFolio(), reporte.getDescripcion(), ciudadano,
                c.organizaciones().get(reporte.getOrganizacionId()), c.personal().get(s.getSenaladoPor()),
                s.getMotivo(), s.getEstado(), (int) Math.max(previos[0] - 1, 0),
                (int) Math.max(previos[1] - aceptadoPropio, 0), s.getCreadoEn());
    }
}
