package mx.uv.taan.comun.seguridad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea la cuenta del administrador la primera vez que arranca el backend, con su contraseña
 * cifrada con BCrypt. Así la contraseña nunca se escribe en un script ni en el repositorio:
 * se pasa en la variable de entorno TAAN_ADMIN_PASSWORD y se exige cambiarla en el primer acceso.
 * La cuenta de coordinación del ayuntamiento la crea después el administrador (CU-12).
 */
@Component
public class InicializadorCuentas implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializadorCuentas.class);

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final String correo;
    private final String password;

    public InicializadorCuentas(JdbcTemplate jdbc, PasswordEncoder passwordEncoder,
                                @Value("${taan.admin.correo}") String correo,
                                @Value("${taan.admin.password:}") String password) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.correo = correo;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        Integer administradores = jdbc.queryForObject(
                "SELECT count(*) FROM usuario WHERE rol = 'administrador'", Integer.class);
        if (administradores != null && administradores > 0) {
            return;
        }
        if (password.isBlank()) {
            log.warn("No hay administrador y TAAN_ADMIN_PASSWORD está vacía: no se creó la cuenta inicial.");
            return;
        }
        jdbc.update("""
                INSERT INTO usuario (correo, password_hash, nombre, rol, credencial_temporal)
                VALUES (?, ?, 'Administración T''AAN', 'administrador', true)
                """, correo, passwordEncoder.encode(password));
        log.info("Cuenta inicial de administrador creada para {} (debe cambiar la contraseña al entrar).", correo);
    }
}
