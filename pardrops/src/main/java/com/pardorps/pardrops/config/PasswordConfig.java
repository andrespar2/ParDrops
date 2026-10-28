package com.pardorps.pardrops.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Cifrado de contraseñas compartido por la autenticación local y la de Google.
 *
 * <p>Se declara en su propia configuración para que los servicios de
 * autenticación puedan usarlo sin crear una dependencia circular con
 * {@link SecurityConfig}.</p>
 */
@Configuration
public class PasswordConfig {

    /**
     * Cifrador de contraseñas basado en BCrypt.
     *
     * @return el codificador a usar al guardar y comprobar contraseñas.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
