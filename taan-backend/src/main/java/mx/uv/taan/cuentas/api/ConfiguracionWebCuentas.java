package mx.uv.taan.cuentas.api;

import java.util.List;

import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import mx.uv.taan.cuentas.aplicacion.UsuarioActual;

/** Registra el resolvedor de UsuarioActual y lo oculta de Swagger (no es un dato que envíe el cliente). */
@Configuration
public class ConfiguracionWebCuentas implements WebMvcConfigurer {

    static {
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(UsuarioActual.class);
    }

    private final UsuarioActualResolver usuarioActual;

    public ConfiguracionWebCuentas(UsuarioActualResolver usuarioActual) {
        this.usuarioActual = usuarioActual;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvedores) {
        resolvedores.add(usuarioActual);
    }
}
