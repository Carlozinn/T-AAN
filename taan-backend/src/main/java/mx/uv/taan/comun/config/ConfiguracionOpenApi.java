package mx.uv.taan.comun.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * Documentación interactiva (springdoc): declara el esquema bearerAuth del contrato para que
 * Swagger UI muestre el botón «Authorize» y se pueda probar la API con el accessToken.
 * Las operaciones públicas siguen funcionando sin token.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "T'AAN - API REST", version = "1.0.0"),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class ConfiguracionOpenApi {
}
