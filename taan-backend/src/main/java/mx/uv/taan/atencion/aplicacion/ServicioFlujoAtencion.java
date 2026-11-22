package mx.uv.taan.atencion.aplicacion;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import mx.uv.taan.administracion.aplicacion.CategoriaVista;
import mx.uv.taan.administracion.aplicacion.ServicioCategorias;
import mx.uv.taan.archivos.aplicacion.ArchivoRef;
import mx.uv.taan.archivos.aplicacion.ServicioArchivos;
import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.configuracion.aplicacion.ServicioAreas;
import mx.uv.taan.cuentas.aplicacion.UsuarioActual;
import mx.uv.taan.cuentas.dominio.Rol;
import mx.uv.taan.reportes.aplicacion.ServicioExpedientes;
import mx.uv.taan.reportes.dominio.EstadoReporte;
import mx.uv.taan.reportes.dominio.HistorialReporte;
import mx.uv.taan.reportes.dominio.MotivoRechazo;
import mx.uv.taan.reportes.dominio.Reporte;

/**
 * Flujo de atención del personal (CU-06, CU-07, CU-08, CU-17, CU-18; RF-019 a RF-023).
 *
 * <p>Cada operación sigue el mismo patrón: bloquear el reporte, comprobar el alcance de quien
 * actúa, aplicar el cambio en la entidad (que valida la transición contra el diagrama de
 * estados) y registrar en el historial un evento público para el ciudadano y, si hay
 * justificación u observaciones internas, una nota que solo ve el personal.</p>
 */
@Service
public class ServicioFlujoAtencion {

    /** No se vincula un duplicado a un reporte que ya no se atiende en la organización. */
    private static final Set<EstadoReporte> PRINCIPAL_NO_VALIDO = EnumSet.of(EstadoReporte.PENDIENTE_CANALIZACION,
            EstadoReporte.DUPLICADO, EstadoReporte.RECHAZADO, EstadoReporte.CANALIZACION_EXTERNA);

    private static final Set<EstadoReporte> ADMITE_EVIDENCIA = EnumSet.of(EstadoReporte.EN_ATENCION,
            EstadoReporte.RESUELTO);

    private final ServicioExpedientes expedientes;
    private final ServicioBandeja bandeja;
    private final ServicioCategorias categorias;
    private final ServicioAreas areas;
    private final ServicioArchivos archivos;

    public ServicioFlujoAtencion(ServicioExpedientes expedientes, ServicioBandeja bandeja,
                                 ServicioCategorias categorias, ServicioAreas areas, ServicioArchivos archivos) {
        this.expedientes = expedientes;
        this.bandeja = bandeja;
        this.categorias = categorias;
        this.areas = areas;
        this.archivos = archivos;
    }

    /**
     * CU-06: Recibido → Asignado si una regla ya determinó el área (FA-04), o → Validado si no;
     * esto último solo la coordinación, que es quien ve los reportes sin área.
     */
    @Transactional
    public ReporteDetalleInstitucional validar(UsuarioActual usuario, String folio, Long categoriaFinalId) {
        Reporte r = bloquearEnAlcance(usuario, folio);
        if (r.getAreaId() == null && usuario.rol() != Rol.coordinacion) {
            throw new ReglaNegocioException(403, "PROHIBIDO", "Solo la coordinación valida reportes sin área.");
        }
        HistorialReporte notaCategoria = null;
        if (categoriaFinalId != null) {
            CategoriaVista categoria = categorias.buscarActiva(categoriaFinalId)
                    .orElseThrow(() -> ReglaNegocioException.noProcesable("CATEGORIA_INVALIDA",
                            "La categoría no existe o está desactivada."));
            r.corregirCategoria(categoriaFinalId);
            notaCategoria = HistorialReporte.nota(r.getId(), "clasificacion",
                    "Categoría final: " + categoria.nombre() + ".", usuario.id());
        }
        EstadoReporte anterior = r.getEstado();
        EstadoReporte nuevo = r.validar();
        String texto = nuevo == EstadoReporte.ASIGNADO
                ? "Tu reporte fue validado y asignado al área " + nombreArea(r.getAreaId()) + "."
                : "Tu reporte fue validado. En breve se asignará al área que lo atenderá.";
        HistorialReporte cambio = HistorialReporte.cambioPorPersonal(r.getId(), anterior, nuevo, "validacion",
                texto, usuario.id());
        if (notaCategoria != null) {
            expedientes.registrar(notaCategoria, cambio);
        } else {
            expedientes.registrar(cambio);
        }
        return bandeja.armarDetalle(r);
    }

    /** CU-06: rechazo con motivo público y justificación interna obligatoria (EX-01). */
    @Transactional
    public ReporteDetalleInstitucional rechazar(UsuarioActual usuario, String folio, MotivoRechazo motivo,
                                                String textoPublico, String justificacion) {
        Reporte r = bloquearEnAlcance(usuario, folio);
        EstadoReporte anterior = r.getEstado();
        String textoLimpio = textoPublico == null || textoPublico.isBlank() ? null : textoPublico.strip();
        r.rechazar(motivo, textoLimpio);
        String publico = "Tu reporte no procede porque " + motivoLegible(motivo) + "."
                + (textoLimpio == null ? "" : " " + textoLimpio);
        expedientes.registrar(
                HistorialReporte.cambioPorPersonal(r.getId(), anterior, EstadoReporte.RECHAZADO, "rechazo", publico, usuario.id()),
                HistorialReporte.nota(r.getId(), "justificacion_rechazo", justificacion.strip(), usuario.id()));
        return bandeja.armarDetalle(r);
    }

    /** CU-06 FA-02: el principal debe ser de la misma organización y seguir vigente (EX-04). */
    @Transactional
    public ReporteDetalleInstitucional marcarDuplicado(UsuarioActual usuario, String folio, String folioPrincipal,
                                                       String observaciones) {
        Reporte r = bloquearEnAlcance(usuario, folio);
        Reporte principal = expedientes.porFolio(folioPrincipal)
                .filter(p -> Objects.equals(p.getOrganizacionId(), usuario.organizacionId()))
                .orElseThrow(() -> ReglaNegocioException.noProcesable("PRINCIPAL_INVALIDO",
                        "El reporte principal no existe en tu organización."));
        if (principal.getId().equals(r.getId())) {
            throw ReglaNegocioException.noProcesable("PRINCIPAL_INVALIDO", "Un reporte no puede ser duplicado de sí mismo.");
        }
        if (PRINCIPAL_NO_VALIDO.contains(principal.getEstado())) {
            throw ReglaNegocioException.noProcesable("PRINCIPAL_INVALIDO",
                    "Elige como principal un reporte vigente: no puede estar duplicado, rechazado ni canalizado fuera.");
        }
        EstadoReporte anterior = r.getEstado();
        r.marcarDuplicado(principal.getId());
        String publico = "Otra persona ya había reportado este mismo problema (folio " + principal.getFolio()
                + "). Tu reporte se unió a ese y aquí puedes seguir su avance.";
        expedientes.registrar(
                HistorialReporte.cambioPorPersonal(r.getId(), anterior, EstadoReporte.DUPLICADO, "duplicado", publico, usuario.id()),
                HistorialReporte.nota(r.getId(), "observaciones_duplicado", observaciones.strip(), usuario.id()));
        return bandeja.armarDetalle(r);
    }

    /**
     * CU-07: asignación de un reporte Validado (solo coordinación) o reasignación de uno Asignado
     * a otra área de la organización, con justificación obligatoria (FA-01).
     */
    @Transactional
    public ReporteDetalleInstitucional asignar(UsuarioActual usuario, String folio, long areaId, String justificacion) {
        Reporte r = bloquearEnAlcance(usuario, folio);
        EstadoReporte estado = r.getEstado();
        boolean reasignacion = estado == EstadoReporte.ASIGNADO;
        if (estado == EstadoReporte.VALIDADO) {
            if (usuario.rol() != Rol.coordinacion) {
                throw new ReglaNegocioException(403, "PROHIBIDO", "Solo la coordinación asigna reportes validados.");
            }
        } else if (reasignacion) {
            if (justificacion == null || justificacion.isBlank()) {
                throw new ReglaNegocioException(400, "VALIDACION", "La reasignación requiere una justificación.");
            }
            if (Objects.equals(r.getAreaId(), areaId)) {
                throw ReglaNegocioException.noProcesable("MISMA_AREA", "El reporte ya está asignado a esa área.");
            }
        } else {
            throw new ReglaNegocioException(409, "TRANSICION_INVALIDA",
                    "Solo se asigna un reporte Validado o se reasigna uno Asignado.");
        }
        areas.exigirActiva(usuario.organizacionId(), areaId);
        Long areaAnterior = r.getAreaId();
        r.asignar(areaId);
        String publico = (reasignacion ? "Tu reporte pasó al área " : "Tu reporte fue asignado al área ")
                + nombreArea(areaId) + ".";
        HistorialReporte cambio = HistorialReporte.cambioDeArea(r.getId(), estado,
                reasignacion ? "reasignacion" : "asignacion", areaAnterior, areaId, publico, usuario.id());
        if (justificacion != null && !justificacion.isBlank()) {
            expedientes.registrar(cambio, HistorialReporte.nota(r.getId(),
                    reasignacion ? "justificacion_reasignacion" : "observaciones_asignacion", justificacion.strip(), usuario.id()));
        } else {
            expedientes.registrar(cambio);
        }
        return bandeja.armarDetalle(r);
    }

    /** CU-08: Asignado o Reabierto → En atención. */
    @Transactional
    public ReporteDetalleInstitucional iniciarAtencion(UsuarioActual usuario, String folio) {
        Reporte r = bloquearEnAlcance(usuario, folio);
        EstadoReporte anterior = r.getEstado();
        r.iniciarAtencion();
        expedientes.registrar(HistorialReporte.cambioPorPersonal(r.getId(), anterior, EstadoReporte.EN_ATENCION,
                "inicio_atencion", "El área " + nombreArea(r.getAreaId()) + " comenzó a atender tu reporte.", usuario.id()));
        return bandeja.armarDetalle(r);
    }

    /** CU-17: En atención → Resuelto; la observación de cierre la ve el ciudadano (RF-042). */
    @Transactional
    public ReporteDetalleInstitucional resolver(UsuarioActual usuario, String folio, String observacionCierre) {
        Reporte r = bloquearEnAlcance(usuario, folio);
        EstadoReporte anterior = r.getEstado();
        r.resolver();
        expedientes.registrar(HistorialReporte.cambioPorPersonal(r.getId(), anterior, EstadoReporte.RESUELTO,
                "resolucion", "Tu reporte fue resuelto: " + observacionCierre.strip(), usuario.id()));
        return bandeja.armarDetalle(r);
    }

    /** CU-18: evidencia de atención, solo mientras se atiende o ya resuelto; sin metadatos (RNF-017). */
    @Transactional
    public ArchivoRef adjuntarEvidencia(UsuarioActual usuario, String folio, MultipartFile archivo) {
        Reporte r = bloquearEnAlcance(usuario, folio);
        if (!ADMITE_EVIDENCIA.contains(r.getEstado())) {
            throw ReglaNegocioException.conflicto("ESTADO_SIN_EVIDENCIA",
                    "Solo se adjunta evidencia a un reporte En atención o Resuelto.");
        }
        ArchivoRef evidencia = archivos.guardarEvidenciaAtencion(r.getId(), archivo, usuario.id())
                .orElseThrow(() -> new ReglaNegocioException(400, "ARCHIVO_INVALIDO",
                        "El archivo debe ser una imagen JPEG, PNG o WebP válida."));
        expedientes.registrar(HistorialReporte.nota(r.getId(), "evidencia_atencion",
                "Se adjuntó evidencia de atención.", usuario.id()));
        return evidencia;
    }

    private Reporte bloquearEnAlcance(UsuarioActual usuario, String folio) {
        Reporte r = expedientes.bloquearPorFolio(folio);
        if (!ServicioBandeja.estaEnAlcance(usuario, r)) {
            throw ReglaNegocioException.noEncontrado();
        }
        return r;
    }

    private String nombreArea(Long areaId) {
        return areaId == null ? "responsable" : areas.nombres(Set.of(areaId)).getOrDefault(areaId, "responsable");
    }

    /** Texto público de cada motivo del catálogo (RF-019, punto 8). */
    static String motivoLegible(MotivoRechazo motivo) {
        return switch (motivo) {
            case no_corresponde -> "no corresponde a esta institución";
            case informacion_insuficiente -> "la información es insuficiente para atenderlo";
            case ya_atendido -> "el problema ya fue atendido";
            case no_servicio_publico -> "no es un problema de servicio público";
        };
    }
}
