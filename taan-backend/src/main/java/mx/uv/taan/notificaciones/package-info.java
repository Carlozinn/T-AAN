/**
 * Avisos push (Firebase Cloud Messaging) y por correo en cada novedad visible de un reporte.
 *
 * <p>Casos de uso: CU-02, CU-25. Módulo del contrato (TAAN_API.yaml): -.</p>
 *
 * <p>Organización interna sugerida: <b>api</b> (controladores REST), <b>aplicacion</b> (servicios con
 * las reglas de negocio), <b>dominio</b> (entidades JPA y reglas puras) e <b>infraestructura</b>
 * (repositorios y adaptadores externos). Ningún módulo usa directamente los repositorios de otro:
 * se comunican a través de sus servicios.</p>
 */
package mx.uv.taan.notificaciones;
