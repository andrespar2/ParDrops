package com.pardorps.pardrops.config;

import com.pardorps.pardrops.security.JwtAuthenticationFilter;
import com.pardorps.pardrops.security.RestAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuración de seguridad de la API.
 *
 * <p>Define un esquema sin sesiones (stateless) basado en JWT: determina qué
 * rutas son públicas, cuáles requieren sesión y cuáles quedan reservadas al
 * administrador. También registra el cifrado de contraseñas y el CORS para que
 * el frontend Angular pueda consumir la API.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /** Punto de entrada que responde 401 en JSON a las peticiones sin sesión. */
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    /** Orígenes permitidos para CORS, separados por comas. */
    private final String allowedOrigins;

    /**
     * @param jwtAuthenticationFilter    filtro que valida el JWT de cada petición.
     * @param authenticationEntryPoint   respuesta JSON para peticiones sin sesión.
     * @param allowedOrigins             orígenes permitidos (config de la app).
     */
    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            @Value("${app.cors.allowed-origins}") String allowedOrigins
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.allowedOrigins = allowedOrigins;
    }

    /**
     * Define la cadena de filtros y las reglas de acceso por ruta.
     *
     * @param http constructor de la configuración HTTP de seguridad.
     * @return la cadena de filtros configurada.
     * @throws Exception si ocurre un error al construir la configuración.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // API sin cookies de sesión: se desactiva CSRF y CORS se define aparte.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Las peticiones sin sesión a rutas protegidas responden 401 en JSON.
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(authenticationEntryPoint)
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // --- Autenticación: registro y login son públicos. ---
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()

                        // --- Catálogo: lectura pública, gestión solo para admin. ---
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")

                        // --- Pedidos: crear es público (invitado o cliente). ---
                        .requestMatchers(HttpMethod.POST, "/api/orders").permitAll()

                        // --- Historial propio: requiere sesión. ---
                        .requestMatchers("/api/auth/me").authenticated()
                        .requestMatchers("/api/orders/me").authenticated()

                        // --- Todos los pedidos: solo administración. ---
                        .requestMatchers(HttpMethod.GET, "/api/orders").hasRole("ADMIN")

                        // --- Cambio de estado de un pedido: solo administración. ---
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/*/status").hasRole("ADMIN")

                        // La consola H2 y cualquier otra ruta quedan abiertas en desarrollo.
                        .anyRequest().permitAll()
                )
                // Registra nuestro filtro JWT antes del filtro estándar de login.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configura el CORS para permitir el acceso desde el frontend Angular.
     *
     * @return fuente de configuración CORS.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Cifrador de contraseñas basado en BCrypt.
     *
     * @return el codificador a usar al guardar y comprobar contraseñas.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Expone el gestor de autenticación para poder validar credenciales en el login.
     *
     * @param configuration configuración de autenticación de Spring.
     * @return el gestor de autenticación.
     * @throws Exception si no se puede obtener.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }
}
