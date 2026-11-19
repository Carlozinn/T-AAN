package mx.uv.taan.administracion.aplicacion;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uv.taan.administracion.dominio.Categoria;
import mx.uv.taan.administracion.dominio.NivelParticipacion;
import mx.uv.taan.administracion.dominio.Organizacion;
import mx.uv.taan.administracion.dominio.Regla;
import mx.uv.taan.administracion.infraestructura.CategoriaRepository;
import mx.uv.taan.administracion.infraestructura.OrganizacionRepository;
import mx.uv.taan.administracion.infraestructura.ReglaRepository;
import mx.uv.taan.administracion.infraestructura.ZonaRepository;
import mx.uv.taan.administracion.infraestructura.ZonaRepository.FilaZona;

/**
 * Motor de reglas de canalización (RF-014, RF-015, RF-037; CU-16 EX-03). Decide a qué
 * organización y área llega un reporte a partir de su ubicación y su categoría. La IA solo
 * propone la categoría; quien decide la organización es este motor (RF-037, punto 3).
 *
 * <ol>
 *   <li>Busca la zona activa que contiene el punto (la de menor superficie si hay varias).</li>
 *   <li>Sin categoría → receptora por defecto, motivo sin_clasificacion.</li>
 *   <li>Toma las reglas activas y vigentes de esa zona y categoría; si es una subcategoría sin
 *       reglas, usa las de su categoría padre. Solo cuentan las de organizaciones activas.</li>
 *   <li>Sin reglas → receptora, sin_regla. Gana la de mayor prioridad; un empate en la
 *       prioridad más alta → receptora, empate_reglas.</li>
 * </ol>
 * <p>Si la organización destino es externa, el reporte irá a canalización externa y sin área.</p>
 */
@Service
public class MotorReglas {

    /** Mismos valores que el tipo motivo_canalizacion (sin «redireccion», que es manual). */
    public enum Motivo { regla, sin_regla, empate_reglas, sin_clasificacion }

    /** Resultado del motor. areaId es null si la regla no define área o si el destino es externo. */
    public record Destino(long zonaId, long organizacionId, String organizacion, Long areaId,
                          boolean externa, Motivo motivo) {
    }

    private final ZonaRepository zonas;
    private final ReglaRepository reglas;
    private final CategoriaRepository categorias;
    private final OrganizacionRepository organizaciones;

    public MotorReglas(ZonaRepository zonas, ReglaRepository reglas, CategoriaRepository categorias,
                       OrganizacionRepository organizaciones) {
        this.zonas = zonas;
        this.reglas = reglas;
        this.categorias = categorias;
        this.organizaciones = organizaciones;
    }

    /** Vacío solo si el punto ya no está en ninguna zona activa (se desactivó tras el envío). */
    @Transactional(readOnly = true)
    public Optional<Destino> determinar(double latitud, double longitud, Long categoriaId) {
        Optional<FilaZona> zona = zonas.zonaActivaQueContiene(latitud, longitud);
        if (zona.isEmpty()) {
            return Optional.empty();
        }
        long zonaId = zona.get().id();
        long receptora = zona.get().organizacionReceptoraId();
        if (categoriaId == null) {
            return Optional.of(destinoA(zonaId, receptora, null, Motivo.sin_clasificacion));
        }
        List<Regla> candidatas = reglasAplicables(zonaId, categoriaId);
        if (candidatas.isEmpty()) {
            Long padre = categorias.findById(categoriaId).map(Categoria::getCategoriaPadreId).orElse(null);
            if (padre != null) {
                candidatas = reglasAplicables(zonaId, padre);
            }
        }
        if (candidatas.isEmpty()) {
            return Optional.of(destinoA(zonaId, receptora, null, Motivo.sin_regla));
        }
        int maxima = candidatas.stream().mapToInt(Regla::getPrioridad).max().orElseThrow();
        List<Regla> ganadoras = candidatas.stream().filter(r -> r.getPrioridad() == maxima).toList();
        if (ganadoras.size() > 1) {
            return Optional.of(destinoA(zonaId, receptora, null, Motivo.empate_reglas));
        }
        Regla regla = ganadoras.get(0);
        return Optional.of(destinoA(zonaId, regla.getOrganizacionId(), regla.getAreaId(), Motivo.regla));
    }

    /** Activas, vigentes hoy y de organizaciones activas. */
    private List<Regla> reglasAplicables(long zonaId, long categoriaId) {
        LocalDate hoy = LocalDate.now(ServicioReglas.ZONA_HORARIA);
        return reglas.findByZonaIdAndCategoriaIdAndActivaTrue(zonaId, categoriaId).stream()
                .filter(r -> !r.getVigenteDesde().isAfter(hoy))
                .filter(r -> r.getVigenteHasta() == null || !r.getVigenteHasta().isBefore(hoy))
                .filter(r -> organizaciones.findById(r.getOrganizacionId()).map(Organizacion::isActiva).orElse(false))
                .toList();
    }

    private Destino destinoA(long zonaId, long organizacionId, Long areaId, Motivo motivo) {
        Optional<Organizacion> organizacion = organizaciones.findById(organizacionId);
        boolean externa = organizacion.map(o -> o.getNivelParticipacion() == NivelParticipacion.externa).orElse(false);
        String nombre = organizacion.map(Organizacion::getNombre).orElse(null);
        return new Destino(zonaId, organizacionId, nombre, externa ? null : areaId, externa, motivo);
    }
}
