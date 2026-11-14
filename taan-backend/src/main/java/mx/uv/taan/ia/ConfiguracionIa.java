package mx.uv.taan.ia;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/** El resto del sistema pide un ClasificadorIa y recibe este: Gemini con respaldo de Groq. */
@Configuration
public class ConfiguracionIa {

    @Bean
    @Primary
    public ClasificadorIa clasificadorIa(@Qualifier("clasificadorGemini") ClasificadorIa principal,
                                         @Qualifier("clasificadorGroq") ClasificadorIa respaldo) {
        return new ClasificadorConRespaldo(principal, respaldo);
    }
}
