/**
 * Adaptador de inteligencia artificial: Gemini como principal y Groq como respaldo. La IA solo propone la clasificación; la organización la decide el motor de reglas.
 *
 * <p>Casos de uso: CU-01 (procesamiento), CU-22. Módulo del contrato (TAAN_API.yaml): -.</p>
 *
 * <p>Organización interna sugerida: <b>api</b> (controladores REST), <b>aplicacion</b> (servicios con
 * las reglas de negocio), <b>dominio</b> (entidades JPA y reglas puras) e <b>infraestructura</b>
 * (repositorios y adaptadores externos). Ningún módulo usa directamente los repositorios de otro:
 * se comunican a través de sus servicios.</p>
 */
package mx.uv.taan.ia;
