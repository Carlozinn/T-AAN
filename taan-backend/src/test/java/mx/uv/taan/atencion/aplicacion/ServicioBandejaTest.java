package mx.uv.taan.atencion.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import mx.uv.taan.administracion.aplicacion.ServicioCategorias;
import mx.uv.taan.administracion.aplicacion.ServicioOrganizaciones;
import mx.uv.taan.archivos.aplicacion.ServicioArchivos;
import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.configuracion.aplicacion.ServicioAreas;
import mx.uv.taan.configuracion.aplicacion.ServicioSemaforo;
import mx.uv.taan.cuentas.aplicacion.ContactoCiudadano;
import mx.uv.taan.cuentas.aplicacion.ServicioCuentasInstitucionales;
import mx.uv.taan.cuentas.aplicacion.ServicioPerfilCiudadano;
import mx.uv.taan.cuentas.aplicacion.UsuarioActual;
import mx.uv.taan.cuentas.dominio.Rol;
import mx.uv.taan.reportes.aplicacion.ServicioExpedientes;
import mx.uv.taan.reportes.dominio.EstadoReporte;
import mx.uv.taan.reportes.dominio.MotivoCanalizacion;
import mx.uv.taan.reportes.dominio.OrigenReporte;
import mx.uv.taan.reportes.dominio.Reporte;

/** Alcance del detalle y datos del ciudadano (RF-018, RF-038) después de una redirección (CU-21). */
class ServicioBandejaTest {

    private static final String FOLIO = "TAAN-2026-000001";
    private static final ContactoCiudadano CONTACTO = new ContactoCiudadano("Ana López", "2721234567");

    private ServicioExpedientes expedientes;
    private ServicioBandeja bandeja;

    private final UsuarioActual coordinacionAnterior = new UsuarioActual(2L, Rol.coordinacion, 1L, null);
    private final UsuarioActual coordinacionDestino = new UsuarioActual(8L, Rol.coordinacion, 2L, null);

    @BeforeEach
    void preparar() {
        expedientes = mock(ServicioExpedientes.class);
        ServicioPerfilCiudadano perfiles = mock(ServicioPerfilCiudadano.class);
        when(perfiles.contacto(7L)).thenReturn(Optional.of(CONTACTO));
        bandeja = new ServicioBandeja(expedientes, mock(ServicioCategorias.class), mock(ServicioAreas.class),
                mock(ServicioOrganizaciones.class), mock(ServicioSemaforo.class), mock(ServicioArchivos.class),
                perfiles, mock(ServicioCuentasInstitucionales.class));

        Reporte r = new Reporte(FOLIO, 7L, "Hay un bache enorme en la carretera estatal", 18.85, -97.06,
                OrigenReporte.web);
        ReflectionTestUtils.setField(r, "id", 100L);
        r.canalizar(12L, false, 1L, 4L, MotivoCanalizacion.regla, EstadoReporte.RECIBIDO);
        r.redirigir(2L, false);
        when(expedientes.porFolio(FOLIO)).thenReturn(Optional.of(r));
    }

    @Test
    void laOrganizacionDestinoVeLosDatosDelCiudadano() {
        assertEquals(CONTACTO, bandeja.detalle(coordinacionDestino, FOLIO).ciudadano());
    }

    @Test
    void laOrganizacionAnteriorYaNoVeElReporte() {
        assertEquals(404, assertThrows(ReglaNegocioException.class,
                () -> bandeja.detalle(coordinacionAnterior, FOLIO)).getEstadoHttp());
    }

    @Test
    void laRespuestaDeLaRedireccionNoTraeLosDatosDelCiudadano() {
        Reporte r = expedientes.porFolio(FOLIO).orElseThrow();

        assertNull(bandeja.armarDetalle(r, false).ciudadano());
    }
}
