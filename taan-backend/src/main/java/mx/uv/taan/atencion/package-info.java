/**
 * Atención institucional: bandeja con semáforo, validación, rechazo, duplicados, asignación, atención, resolución, redirección y señalamientos.
 *
 * <p>Casos de uso: CU-05 a CU-09, CU-17, CU-18, CU-19, CU-21. Módulo del contrato (TAAN_API.yaml): Atención institucional.</p>
 *
 * <p>Organización interna sugerida: <b>api</b> (controladores REST), <b>aplicacion</b> (servicios con
 * las reglas de negocio), <b>dominio</b> (entidades JPA y reglas puras) e <b>infraestructura</b>
 * (repositorios y adaptadores externos). Ningún módulo usa directamente los repositorios de otro:
 * se comunican a través de sus servicios.</p>
 */
package mx.uv.taan.atencion;
