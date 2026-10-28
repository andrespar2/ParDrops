package com.pardorps.pardrops.security;

import com.pardorps.pardrops.exception.GoogleLoginException;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.service.GoogleAuthService;
import com.pardorps.pardrops.service.GoogleLoginCodeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Completa el acceso con Google y entrega la sesión al frontend.
 *
 * <p>Busca o crea la cuenta, emite un código temporal de un solo uso y
 * redirige al frontend. El JWT no se incluye en la URL: el frontend canjea el
 * código por HTTP en {@code POST /api/auth/google/exchange}.</p>
 */
@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    private final GoogleAuthService googleAuthService;
    private final GoogleLoginCodeService codeService;
    private final String frontendUrl;
    private final String successPath;
    private final String failurePath;

    /**
     * @param googleAuthService servicio que crea o vincula la cuenta.
     * @param codeService       emisor de códigos de un solo uso.
     * @param frontendUrl       URL base del frontend.
     * @param successPath       ruta del frontend que canjea el código.
     * @param failurePath       ruta del frontend que muestra el error.
     */
    public OAuth2AuthenticationSuccessHandler(
            GoogleAuthService googleAuthService,
            GoogleLoginCodeService codeService,
            @Value("${app.oauth2.frontend-url:http://localhost:4200}") String frontendUrl,
            @Value("${app.oauth2.success-path:/auth/callback}") String successPath,
            @Value("${app.oauth2.failure-path:/login}") String failurePath
    ) {
        this.googleAuthService = googleAuthService;
        this.codeService = codeService;
        this.frontendUrl = frontendUrl;
        this.successPath = successPath;
        this.failurePath = failurePath;
    }

    /**
     * Resuelve la cuenta y redirige al frontend con el código temporal.
     *
     * @param request        petición actual.
     * @param response       respuesta que se redirige.
     * @param authentication autenticación de Google ya validada.
     * @throws IOException si falla la redirección.
     */
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        try {
            OidcUser oidcUser = extractOidcUser(authentication);
            User user = googleAuthService.resolveUser(oidcUser);
            String code = codeService.issue(user.getId());
            String target = UriComponentsBuilder.fromUriString(frontendUrl)
                    .path(successPath)
                    .queryParam("code", code)
                    .build()
                    .toUriString();
            response.sendRedirect(target);
        } catch (GoogleLoginException ex) {
            log.info("Acceso con Google rechazado: {}", ex.getMessage());
            redirectToFailure(response, ex.getReason());
        } catch (RuntimeException ex) {
            log.error("No se pudo completar el acceso con Google", ex);
            redirectToFailure(response, "error");
        }
    }

    /**
     * Extrae la identidad OIDC de la autenticación.
     *
     * @param authentication autenticación de Spring Security.
     * @return la identidad de Google.
     */
    private OidcUser extractOidcUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
            throw new GoogleLoginException("error", "Google no devolvió una identidad OIDC");
        }
        return oidcUser;
    }

    /**
     * Redirige al frontend con el motivo del error.
     *
     * @param response respuesta HTTP.
     * @param reason   código corto del motivo.
     * @throws IOException si falla la redirección.
     */
    private void redirectToFailure(HttpServletResponse response, String reason) throws IOException {
        String target = UriComponentsBuilder.fromUriString(frontendUrl)
                .path(failurePath)
                .queryParam("oauth", reason)
                .build()
                .toUriString();
        response.sendRedirect(target);
    }
}
