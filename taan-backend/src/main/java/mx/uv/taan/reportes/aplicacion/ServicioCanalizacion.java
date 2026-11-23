package mx.uv.taan.reportes.aplicacion;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uv.taan.administracion.aplicacion.CategoriaVista;
import mx.uv.taan.administracion.aplicacion.MotorReglas;
import mx.uv.taan.administracion.aplicacion.ServicioCategorias;
import mx.uv.taan.ia.ResultadoClasificacion;
import mx.uv.taan.reportes.dominio.EstadoReporte;
import mx.uv.taan.reportes.dominio.HistorialReporte;
import mx.uv.taan.reportes.dominio.MotivoCanalizacion;
import mx.uv.taan.reportes.dominio.Reporte;
import mx.uv.taan.reportes.infraestructura.HistorialReporteRepository;
import mx.uv.taan.reportes.infraestructura.ReporteRepository;
import mx.uv.taan.reportes.infraestructura.ResultadoIaRepository;
import tools.jackson.databind.ObjectMapper;

/**
 * Aplica a un reporte pendiente el resultado de la IA y la decisión del motor de reglas
 * (CU-01 paso 6; RF-014, RF-015, RF-037). La llamada a la IA no ocurre aquí, sino antes y fuera
 * de la transacción (ProcesadorReportes), para no mantener la fila bloqueada mientras se espera
 * al proveedor.
 */
@Service
public class ServicioCanalizacion {

    private static final Logger log = LoggerFactory.getLogger(ServicioCanalizacion.class);

    /** Margen antes de reintentar un pendiente: da tiempo al procesamiento inmediato. */
    private static final Duration ESPERA_REINTENTO = Duration.ofMinutes(2);

    private final ReporteRepository reportes;
    private final HistorialReporteRepository historial;
    private final ResultadoIaRepository resultadosIa;
    private final ServicioCategorias categorias;
    private final MotorReglas motor;
    private final ObjectMapper json;

    public ServicioCanalizacion(ReporteRepository reportes, HistorialReporteRepository historial,
                                ResultadoIaRepository resultadosIa, ServicioCategorias categorias,
                                MotorReglas motor, ObjectMapper json) {
        this.reportes = reportes;
        this.historial = historial;
        this.resultadosIa = resultadosIa;
        this.categorias = categorias;
        this.motor = motor;
        this.json = json;
    }

    /** Descripción del reporte si sigue pendiente (lo único que se envía a la IA: nunca datos personales). */
    @Transactional(readOnly = true)
    public Optional<String> descripcionSiPendiente(long reporteId) {
        return reportes.findById(reporteId)
                .filter(r -> r.getEstado() == EstadoReporte.PENDIENTE_CANALIZACION)
                .map(Reporte::getDescripcion);
    }

    @Transactional(readOnly = true)
    public List<Long> pendientesParaReintentar() {
        return reportes.pendientesCreadosAntesDe(Instant.now().minus(ESPERA_REINTENTO));
    }

    /** Catálogo vigente en el formato que recibe la IA: «Categoría» y «Categoría / Subcategoría». */
    @Transactional(readOnly = true)
    public List<String> catalogoParaIa() {
        List<CategoriaVista> activas = categorias.listarActivas();
        Map<Long, String> raices = new LinkedHashMap<>();
        activas.stream().filter(c -> c.categoriaPadreId() == null).forEach(c -> raices.put(c.id(), c.nombre()));
        return activas.stream()
                .filter(c -> c.categoriaPadreId() == null || raices.containsKey(c.categoriaPadreId()))
                .map(c -> c.categoriaPadreId() == null ? c.nombre() : raices.get(c.categoriaPadreId()) + " / " + c.nombre())
                .sorted()
                .toList();
    }

    /**
     * Cuenta un intento en que la IA no respondió (RF-007 punto 5).
     *
     * @return true si ya se agotaron los intentos y hay que canalizarlo sin clasificación
     */
    @Transactional
    public boolean registrarFalloIa(long reporteId, int intentosMaximos) {
        Reporte reporte = reportes.bloquear(reporteId).orElse(null);
        if (reporte == null || reporte.getEstado() != EstadoReporte.PENDIENTE_CANALIZACION) {
            return false;
        }
        reporte.registrarIntentoIa();
        return reporte.getIntentosIa() >= intentosMaximos;
    }

    /**
     * Canaliza el reporte. Si ya no está pendiente (otro proceso lo canalizó) no hace nada. Si su
     * ubicación ya no está en ninguna zona activa, lo deja pendiente y lo avisa en el log.
     * intentos_ia cuenta las llamadas a la IA: solo suma si el resultado viene de un proveedor.
     */
    @Transactional
    public void aplicar(long reporteId, ResultadoClasificacion resultado) {
        Reporte reporte = reportes.bloquear(reporteId).orElse(null);
        if (reporte == null || reporte.getEstado() != EstadoReporte.PENDIENTE_CANALIZACION) {
            return;
        }
        if (resultado.proveedor() != null) {
            reporte.registrarIntentoIa();
        }
        Long categoriaId = resolverCategoria(resultado);
        Optional<MotorReglas.Destino> destino = motor.determinar(reporte.getLatitud(), reporte.getLongitud(), categoriaId);
        if (destino.isEmpty()) {
            log.warn("Reporte {} sin zona activa en su ubicación: queda pendiente de canalización.", reporte.getFolio());
            return;
        }
        MotorReglas.Destino d = destino.get();
        EstadoReporte nuevoEstado = d.externa() ? EstadoReporte.CANALIZACION_EXTERNA : EstadoReporte.RECIBIDO;
        reporte.canalizar(categoriaId, resultado.posibleContenidoNoValido(), d.organizacionId(), d.areaId(),
                MotivoCanalizacion.valueOf(d.motivo().name()), nuevoEstado);
        resultadosIa.guardar(reporteId, comoJson(resultado, categoriaId, reporte.getIntentosIa()));
        historial.save(HistorialReporte.canalizacion(reporteId, nuevoEstado, d.organizacionId(), d.areaId(),
                textoPublico(d)));
        log.info("Reporte {} canalizado a {} ({}, motivo {}).", reporte.getFolio(), d.organizacion(),
                nuevoEstado.valorBd(), d.motivo());
    }

    /**
     * Convierte la propuesta de la IA (nombres) en el id del catálogo. Si la IA nombra algo que no
     * existe o está desactivado, se trata como «sin clasificación»: la IA nunca inventa categorías.
     */
    Long resolverCategoria(ResultadoClasificacion resultado) {
        if (resultado == null || !resultado.clasificado()) {
            return null;
        }
        List<CategoriaVista> activas = categorias.listarActivas();
        Optional<CategoriaVista> raiz = activas.stream()
                .filter(c -> c.categoriaPadreId() == null && c.nombre().equalsIgnoreCase(resultado.categoria().strip()))
                .findFirst();
        if (raiz.isEmpty()) {
            return null;
        }
        if (resultado.subcategoria() == null || resultado.subcategoria().isBlank()) {
            return raiz.get().id();
        }
        return activas.stream()
                .filter(c -> Objects.equals(c.categoriaPadreId(), raiz.get().id()))
                .filter(c -> c.nombre().equalsIgnoreCase(resultado.subcategoria().strip()))
                .map(CategoriaVista::id)
                .findFirst()
                .orElse(raiz.get().id());
    }

    /** Lo que lee el ciudadano en su historial. Nunca afirma que una institución externa lo recibió (RF-015). */
    private static String textoPublico(MotorReglas.Destino d) {
        return d.externa()
                ? "Este problema lo atiende " + d.organizacion() + ", que no opera dentro de T'AAN. "
                        + "Consulta sus canales oficiales para continuar tu trámite."
                : "Tu reporte quedó a cargo de " + d.organizacion() + ", que lo revisará.";
    }

    /**
     * Lo que propuso la IA, para trazabilidad (columna resultado_ia). No guarda la descripción ni
     * datos personales. razon explica por qué no hay categoría.
     */
    String comoJson(ResultadoClasificacion resultado, Long categoriaId, int intentos) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("proveedor", resultado.proveedor());
        datos.put("modelo", resultado.modelo());
        datos.put("categoria", resultado.categoria());
        datos.put("subcategoria", resultado.subcategoria());
        datos.put("categoriaId", categoriaId);
        datos.put("posibleContenidoNoValido", resultado.posibleContenidoNoValido());
        datos.put("motivoContenido", resultado.motivoContenido());
        datos.put("intentos", intentos);
        datos.put("fecha", Instant.now().toString());
        if (categoriaId == null) {
            // Si la IA nombró una categoría que se desactivó mientras tanto, cuenta como sin categoría.
            datos.put("razon", resultado.razon() != null ? resultado.razon() : ResultadoClasificacion.SIN_CATEGORIA);
        }
        return json.writeValueAsString(datos);
    }
}
