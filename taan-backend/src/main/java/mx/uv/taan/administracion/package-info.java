/**
 * Administración técnica: organizaciones, categorías, zonas (PostGIS), reglas de canalización, canales externos y resolución de señalamientos.
 *
 * <p>Casos de uso: CU-11, CU-14, CU-15, CU-16, CU-20, CU-26. Módulo del contrato (TAAN_API.yaml): Administración técnica.</p>
 *
 * <p>Organización interna sugerida: <b>api</b> (controladores REST), <b>aplicacion</b> (servicios con
 * las reglas de negocio), <b>dominio</b> (entidades JPA y reglas puras) e <b>infraestructura</b>
 * (repositorios y adaptadores externos). Ningún módulo usa directamente los repositorios de otro:
 * se comunican a través de sus servicios.</p>
 */
package mx.uv.taan.administracion;
