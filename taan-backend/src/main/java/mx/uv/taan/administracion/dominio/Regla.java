package mx.uv.taan.administracion.dominio;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Regla de canalización: territorio + categoría → organización y, opcionalmente, área
 * (tabla regla_canalizacion; RF-030, CU-16). Gana la de mayor prioridad; un empate equivale
 * a no tener regla (lo resuelve la canalización en el incremento 3).
 *
 * <p>Zona, categoría y organización no cambian una vez creada: para otra combinación se
 * cierra la vigencia o se desactiva y se crea otra, así el historial sigue siendo legible.</p>
 */
@Entity
@Table(name = "regla_canalizacion")
public class Regla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zona_id", nullable = false, updatable = false)
    private Long zonaId;

    @Column(name = "categoria_id", nullable = false, updatable = false)
    private Long categoriaId;

    @Column(name = "organizacion_id", nullable = false, updatable = false)
    private Long organizacionId;

    /** Área destino; la define la coordinación de la organización (CU-22). */
    @Column(name = "area_id")
    private Long areaId;

    @Column(nullable = false)
    private int prioridad;

    @Column(name = "vigente_desde", nullable = false, updatable = false)
    private LocalDate vigenteDesde;

    @Column(name = "vigente_hasta")
    private LocalDate vigenteHasta;

    @Column(nullable = false)
    private boolean activa = true;

    protected Regla() {
        // Requerido por JPA.
    }

    public Regla(Long zonaId, Long categoriaId, Long organizacionId, Long areaId, int prioridad,
                 LocalDate vigenteDesde, LocalDate vigenteHasta) {
        this.zonaId = zonaId;
        this.categoriaId = categoriaId;
        this.organizacionId = organizacionId;
        this.areaId = areaId;
        this.prioridad = prioridad;
        this.vigenteDesde = vigenteDesde;
        this.vigenteHasta = vigenteHasta;
    }

    public void cambiarPrioridad(int prioridad) {
        this.prioridad = prioridad;
    }

    public void cambiarArea(Long areaId) {
        this.areaId = areaId;
    }

    public void cerrarVigencia(LocalDate vigenteHasta) {
        this.vigenteHasta = vigenteHasta;
    }

    public void cambiarActiva(boolean activa) {
        this.activa = activa;
    }

    public Long getId() {
        return id;
    }

    public Long getZonaId() {
        return zonaId;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public Long getOrganizacionId() {
        return organizacionId;
    }

    public Long getAreaId() {
        return areaId;
    }

    public int getPrioridad() {
        return prioridad;
    }

    public LocalDate getVigenteDesde() {
        return vigenteDesde;
    }

    public LocalDate getVigenteHasta() {
        return vigenteHasta;
    }

    public boolean isActiva() {
        return activa;
    }
}
