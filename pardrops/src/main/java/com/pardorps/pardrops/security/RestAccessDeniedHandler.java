package com.pardorps.pardrops.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Respuesta devuelta cuando el usuario está autenticado pero no tiene permiso.
 *
 * <p>El manejador por defecto de Spring Security solo cambia el código de
 * estado a 403 y deja el cuerpo vacío. En una API REST eso provoca que el
 * contenedor.servlet realice una segunda pasada (un {@code ERROR dispatch}) para
 * renderizar la página de error, y en esa segunda pasada la petición vuelve a
 * la cadena de filtros sin cabecera de autenticación: el resultado es que el
 * 403 se acaba viendo como un 401 "se requiere iniciar sesión".</p>
 *
 * <p>Escribiendo el cuerpo JSON se confirma la respuesta, no hay segunda
 * pasada y el cliente recibe el 403 real con su mensaje.</p>
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    /**
     * Escribe una respuesta 403 con un cuerpo JSON sencillo.
     */
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"message\":\"No tienes permisos para realizar esta acción\"}");
    }
}
