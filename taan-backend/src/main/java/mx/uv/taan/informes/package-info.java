/**
 * Estadísticas e informes exportables (PDF y hoja de cálculo) sin datos personales.
 *
 * <p>Casos de uso: CU-10. Módulo del contrato (TAAN_API.yaml): Estadísticas e informes.</p>
 *
 * <p>Organización interna sugerida: <b>api</b> (controladores REST), <b>aplicacion</b> (servicios con
 * las reglas de negocio), <b>dominio</b> (entidades JPA y reglas puras) e <b>infraestructura</b>
 * (repositorios y adaptadores externos). Ningún módulo usa directamente los repositorios de otro:
 * se comunican a través de sus servicios.</p>
 */
package mx.uv.taan.informes;
