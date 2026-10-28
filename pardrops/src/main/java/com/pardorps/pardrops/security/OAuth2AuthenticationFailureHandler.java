package com.pardorps.pardrops.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Redirige al frontend cuando Google rechaza o cancela el acceso.
 *
 * <p>No se muestra el detalle técnico al usuario; solo un código corto que la
 * pantalla de acceso traduce a un mensaje comprensible.</p>
 */
@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler.class);

    private final String frontendUrl;
    private final String failurePath;

    /**
     * @param frontendUrl URL base del frontend.
     * @param failurePath ruta del frontend que muestra el error.
     */
    public OAuth2AuthenticationFailureHandler(
            @Value("${app.oauth2.frontend-url:http://localhost:4200}") String frontendUrl,
            @Value("${app.oauth2.failure-path:/login}") String failurePath
    ) {
        this.frontendUrl = frontendUrl;
        this.failurePath = failurePath;
    }

    /**
     * Redirige al frontend con el motivo del fallo.
     *
     * @param request   petición actual.
     * @param response  respuesta que se redirige.
     * @param exception error devuelto por Spring Security.
     * @throws IOException si falla la redirección.
     */
    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        String reason = "error";
        if (exception instanceof OAuth2AuthenticationException oauth2Exception
                && "access_denied".equals(oauth2Exception.getError().getErrorCode())) {
            reason = "cancelado";
        }
        log.info("Google rechazó el acceso: {}", exception.getMessage());

        String target = UriComponentsBuilder.fromUriString(frontendUrl)
                .path(failurePath)
                .queryParam("oauth", reason)
                .build()
                .toUriString();
        response.sendRedirect(target);
    }
}
