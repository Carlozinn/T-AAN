/**
 * Adaptador de inteligencia artificial: Gemini como principal y Groq como respaldo. Clasifica los
 * reportes y propone la correspondencia categoría → área; solo propone: la organización la decide el
 * motor de reglas y la correspondencia la confirma la coordinación.
 *
 * <p>Casos de uso: CU-01 (procesamiento), CU-22. Módulo del contrato (TAAN_API.yaml): -.</p>
 *
 * <p>Organización interna sugerida: <b>api</b> (controladores REST), <b>aplicacion</b> (servicios con
 * las reglas de negocio), <b>dominio</b> (entidades JPA y reglas puras) e <b>infraestructura</b>
 * (repositorios y adaptadores externos). Ningún módulo usa directamente los repositorios de otro:
 * se comunican a través de sus servicios.</p>
 */
package mx.uv.taan.ia;
