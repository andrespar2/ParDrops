package com.pardorps.pardrops.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Redirige el dominio raíz al frontend canónico.
 *
 * <p>El dominio {@code pardrops.qd.je} es el apex de su zona DNS, donde no se
 * puede publicar un CNAME hacia Cloudflare Pages. Para que la URL corta
 * funcione, el apex apunta con un registro {@code A} a Render y este filtro
 * responde con un 301 hacia {@code www.pardrops.qd.je} (donde sí vive la SPA en
 * Cloudflare). Las rutas de la API, las imágenes y OAuth2 se sirven sin
 * redirección.</p>
 *
 * <p>Si {@code app.redirect.apex-host} está vacío el filtro no hace nada, de
 * modo que en local y en los tests el comportamiento no cambia.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApexRedirectFilter extends OncePerRequestFilter {

    /** Rutas que nunca se redirigen: son contenido del propio backend. */
    private static final List<String> BACKEND_PREFIXES =
            List.of("/api", "/media", "/oauth2", "/login/oauth2", "/actuator", "/h2-console");

    /** Host cuyo tráfico se redirige (por ejemplo {@code pardrops.qd.je}). */
    private final String apexHost;

    /** URL del frontend canónico, sin barra final. */
    private final String targetUrl;

    /**
     * @param apexHost    host del apex, o vacío para desactivar la redirección.
     * @param frontendUrl URL pública del frontend.
     */
    public ApexRedirectFilter(
            @Value("${app.redirect.apex-host:}") String apexHost,
            @Value("${app.oauth2.frontend-url:http://localhost:4200}") String frontendUrl
    ) {
        this.apexHost = apexHost == null ? "" : apexHost.trim().toLowerCase();
        this.targetUrl = frontendUrl.endsWith("/")
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : frontendUrl;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (apexHost.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String host = request.getServerName();
        String path = request.getRequestURI();
        boolean backendPath = BACKEND_PREFIXES.stream().anyMatch(path::startsWith);

        if (host != null && host.equalsIgnoreCase(apexHost) && !backendPath) {
            String query = request.getQueryString();
            String location = targetUrl + path + (query == null ? "" : "?" + query);
            response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            response.setHeader("Location", location);
            response.setHeader("Cache-Control", "public, max-age=3600");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
