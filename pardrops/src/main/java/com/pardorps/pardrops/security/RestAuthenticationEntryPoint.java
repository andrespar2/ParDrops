package com.pardorps.pardrops.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Respuesta devuelta cuando se intenta acceder a un recurso protegido sin
 * autenticación.
 *
 * <p>Por defecto Spring Security redirige al login HTML; al tratarse de una API
 * REST, aquí se responde con un 401 en formato JSON para que el frontend pueda
 * gestionarlo de forma uniforme.</p>
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * Escribe una respuesta 401 con un cuerpo JSON sencillo.
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"Se requiere iniciar sesión\"}");
    }
}
