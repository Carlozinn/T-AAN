package mx.uv.taan.comun.seguridad;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Seguridad de la API (RF-002, RF-003). Sesiones sin estado con JWT firmado por el propio
 * backend; el rol viaja en el campo "rol" del token (ciudadano, personal_area,
 * coordinacion, administrador), igual que en el contrato.
 *
 * Las rutas son relativas a /api/v1 (server.servlet.context-path en application.yml).
 * Esta es la primera barrera; la segunda (que el reporte sea de la organización o del
 * ciudadano que lo pide) la aplica cada servicio.
 */
@Configuration
@EnableMethodSecurity
public class ConfiguracionSeguridad {

    @Bean
    public SecurityFilterChain cadenaDeSeguridad(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                // Cambio de contraseña institucional: cualquier rol del personal, nunca un ciudadano.
                .requestMatchers("/auth/institucional/password")
                    .hasAnyRole("personal_area", "coordinacion", "administrador")
                // Operaciones públicas del contrato
                .requestMatchers("/auth/ciudadanos/**", "/auth/institucional/sesion", "/auth/refresco").permitAll()
                // Documentación interactiva (springdoc)
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/ciudadano/**", "/cobertura").hasRole("ciudadano")
                .requestMatchers("/institucional/**").hasAnyRole("personal_area", "coordinacion")
                .requestMatchers("/organizacion/**").hasRole("coordinacion")
                .requestMatchers("/admin/**").hasRole("administrador")
                .anyRequest().authenticated())
            // 401 y 403 con el esquema Error del contrato, también para el recurso protegido.
            .exceptionHandling(e -> e
                .authenticationEntryPoint(sinSesion())
                .accessDeniedHandler(sinPermiso()))
            .oauth2ResourceServer(o -> o
                .authenticationEntryPoint(sinSesion())
                .accessDeniedHandler(sinPermiso())
                .jwt(j -> j.jwtAuthenticationConverter(convertidorDeRoles())));
        return http.build();
    }

    /** Convierte el campo "rol" del JWT en la autoridad ROLE_<rol>. */
    private JwtAuthenticationConverter convertidorDeRoles() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName(JwtTaan.CLAIM_ROL);
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(roles);
        return convertidor;
    }

    /**
     * Clave HMAC para firmar y verificar los tokens (al menos 32 caracteres). La usa el JwtDecoder de
     * abajo y el emisor de tokens del módulo cuentas (ServicioTokens) al iniciar sesión.
     */
    @Bean
    public SecretKey claveJwt(@Value("${taan.jwt.secreto}") String secreto) {
        return new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /**
     * Decodificador del recurso protegido: además de firma y vigencia exige tipo = acceso,
     * para que un refresco o un enlace de verificación jamás autentique una petición.
     */
    @Bean
    public JwtDecoder jwtDecoder(SecretKey claveJwt) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(claveJwt).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> soloAcceso = jwt ->
                JwtTaan.TIPO_ACCESO.equals(jwt.getClaimAsString(JwtTaan.CLAIM_TIPO))
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(
                                new OAuth2Error("invalid_token", "No es un token de acceso", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(), soloAcceso));
        return decoder;
    }

    /** Contraseñas siempre con BCrypt (RNF-006). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Orígenes permitidos: la web en desarrollo y la app con Capacitor. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${taan.cors.origenes}") List<String> origenes) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(origenes);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setExposedHeaders(List.of("Retry-After"));
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", cors);
        return fuente;
    }

    private AuthenticationEntryPoint sinSesion() {
        return (peticion, respuesta, error) ->
                escribir(respuesta, 401, "NO_AUTENTICADO", "Inicia sesión para continuar.");
    }

    private AccessDeniedHandler sinPermiso() {
        return (peticion, respuesta, error) ->
                escribir(respuesta, 403, "PROHIBIDO", "Tu cuenta no puede realizar esta operación.");
    }

    /**
     * Escribe el esquema Error del contrato a mano: estos filtros corren antes de Spring MVC y
     * así no dependen de la versión de Jackson (Spring Boot 4 usa Jackson 3). Los textos son
     * fijos y sin datos del usuario, por eso basta con escapar comillas y diagonales.
     */
    private void escribir(HttpServletResponse respuesta, int estado, String codigo, String mensaje)
            throws java.io.IOException {
        respuesta.setStatus(estado);
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String textoSeguro = mensaje.replace("\\", "\\\\").replace("\"", "\\\"");
        respuesta.getWriter().write(
                "{\"codigo\":\"" + codigo + "\",\"mensaje\":\"" + textoSeguro + "\",\"detalles\":[]}");
    }
}
