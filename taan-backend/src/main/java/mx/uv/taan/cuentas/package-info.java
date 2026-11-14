/**
 * Cuentas ciudadanas: registro con Google o correo verificado, perfil, teléfono, avisos, dispositivos, eliminación de cuenta y suspensiones.
 *
 * <p>Casos de uso: CU-04, CU-24, CU-25, CU-26. Módulo del contrato (TAAN_API.yaml): Autenticación, Cuenta ciudadana.</p>
 *
 * <p>Organización interna sugerida: <b>api</b> (controladores REST), <b>aplicacion</b> (servicios con
 * las reglas de negocio), <b>dominio</b> (entidades JPA y reglas puras) e <b>infraestructura</b>
 * (repositorios y adaptadores externos). Ningún módulo usa directamente los repositorios de otro:
 * se comunican a través de sus servicios.</p>
 */
package mx.uv.taan.cuentas;
