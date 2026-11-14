/**
 * Reportes ciudadanos: cobertura, registro con folio, procesamiento asíncrono (IA y reglas), «Mis reportes», reapertura y la máquina de estados (paquete dominio).
 *
 * <p>Casos de uso: CU-01, CU-02, CU-03, CU-23. Módulo del contrato (TAAN_API.yaml): Reportes ciudadanos.</p>
 *
 * <p>Organización interna sugerida: <b>api</b> (controladores REST), <b>aplicacion</b> (servicios con
 * las reglas de negocio), <b>dominio</b> (entidades JPA y reglas puras) e <b>infraestructura</b>
 * (repositorios y adaptadores externos). Ningún módulo usa directamente los repositorios de otro:
 * se comunican a través de sus servicios.</p>
 */
package mx.uv.taan.reportes;
