package com.pardorps.pardrops.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;
import org.springframework.web.util.WebUtils;

import java.time.Duration;
import java.util.Base64;

/**
 * Guarda la solicitud de autorización de OAuth2 en una cookie.
 *
 * <p>La API es stateless, así que no se abre una sesión para recordar el
 * {@code state} y el {@code PKCE} entre la ida y la vuelta a Google. La cookie
 * es {@code HttpOnly}, {@code SameSite=Lax}, dura cinco minutos y se elimina al
 * completar o cancelar el flujo.</p>
 */
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    /** Nombre de la cookie que transporta la solicitud. */
    public static final String COOKIE_NAME = "pardrops_oauth2_request";

    /** Vida máxima de la cookie en segundos. */
    private static final int MAX_AGE_SECONDS = 300;

    /** Indica si la cookie debe exigir HTTPS. */
    private final boolean secureCookies;

    /**
     * @param secureCookies {@code true} para marcar la cookie como {@code Secure}.
     */
    public HttpCookieOAuth2AuthorizationRequestRepository(
            @Value("${app.oauth2.secure-cookies:false}") boolean secureCookies
    ) {
        this.secureCookies = secureCookies;
    }

    /**
     * Recupera la solicitud guardada.
     *
     * @param request petición actual.
     * @return la solicitud o {@code null} si no hay cookie válida.
     */
    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, COOKIE_NAME);
        if (cookie == null || cookie.getValue().isBlank()) {
            return null;
        }
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(cookie.getValue());
            return (OAuth2AuthorizationRequest) SerializationUtils.deserialize(bytes);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /**
     * Guarda la solicitud o elimina la cookie cuando la solicitud es nula.
     *
     * @param authorizationRequest solicitud a guardar (puede ser nula).
     * @param request              petición actual.
     * @param response             respuesta donde se añade la cookie.
     */
    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (authorizationRequest == null) {
            deleteCookie(request, response);
            return;
        }
        byte[] bytes = SerializationUtils.serialize(authorizationRequest);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, value)
                .path("/")
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite("Lax")
                .maxAge(Duration.ofSeconds(MAX_AGE_SECONDS))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * Carga la solicitud y elimina la cookie de inmediato.
     *
     * @param request  petición actual.
     * @param response respuesta donde se borra la cookie.
     * @return la solicitud guardada o {@code null}.
     */
    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        deleteCookie(request, response);
        return authorizationRequest;
    }

    /**
     * Elimina la cookie del navegador.
     *
     * @param request  petición actual.
     * @param response respuesta donde se envía la cookie vacía.
     */
    private void deleteCookie(HttpServletRequest request, HttpServletResponse response) {
        if (WebUtils.getCookie(request, COOKIE_NAME) == null) {
            return;
        }
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, "")
                .path("/")
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite("Lax")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
