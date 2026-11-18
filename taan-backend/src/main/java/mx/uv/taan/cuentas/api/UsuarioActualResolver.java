package mx.uv.taan.cuentas.api;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.comun.seguridad.JwtTaan;
import mx.uv.taan.cuentas.aplicacion.ServicioCuentasInstitucionales;
import mx.uv.taan.cuentas.aplicacion.UsuarioActual;
import mx.uv.taan.cuentas.dominio.Rol;

/**
 * Entrega un {@link UsuarioActual} a cualquier controlador que lo declare como parámetro.
 * Evita repetir en cada endpoint institucional «leer el token, buscar la cuenta, verificar
 * que siga activa y tomar su organización».
 */
@Component
public class UsuarioActualResolver implements HandlerMethodArgumentResolver {

    private final ServicioCuentasInstitucionales cuentas;

    public UsuarioActualResolver(ServicioCuentasInstitucionales cuentas) {
        this.cuentas = cuentas;
    }

    @Override
    public boolean supportsParameter(MethodParameter parametro) {
        return UsuarioActual.class.equals(parametro.getParameterType());
    }

    @Override
    public UsuarioActual resolveArgument(MethodParameter parametro, ModelAndViewContainer modelo,
                                         NativeWebRequest peticion, WebDataBinderFactory binder) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !(autenticacion.getPrincipal() instanceof Jwt jwt)) {
            throw new ReglaNegocioException(401, "NO_AUTENTICADO", "Inicia sesión para continuar.");
        }
        if (Rol.ciudadano.name().equals(jwt.getClaimAsString(JwtTaan.CLAIM_ROL))) {
            throw new ReglaNegocioException(403, "PROHIBIDO", "Tu cuenta no puede realizar esta operación.");
        }
        return cuentas.identidadActiva(Long.parseLong(jwt.getSubject()));
    }
}
