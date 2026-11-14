/**
 * Configuración de cada organización por su coordinación: áreas, personal, semáforo y correspondencia categoría-área (plantilla municipal).
 *
 * <p>Casos de uso: CU-12, CU-13, CU-22. Módulo del contrato (TAAN_API.yaml): Configuración institucional.</p>
 *
 * <p>Organización interna sugerida: <b>api</b> (controladores REST), <b>aplicacion</b> (servicios con
 * las reglas de negocio), <b>dominio</b> (entidades JPA y reglas puras) e <b>infraestructura</b>
 * (repositorios y adaptadores externos). Ningún módulo usa directamente los repositorios de otro:
 * se comunican a través de sus servicios.</p>
 */
package mx.uv.taan.configuracion;
