package com.pardorps.pardrops.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprueba la configuración del flujo OAuth2 cuando hay credenciales.
 *
 * <p>No se contacta con Google: solo se valida que el backend redirige al
 * endpoint de autorización correcto y que guarda el estado en la cookie.</p>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "GOOGLE_CLIENT_ID=cliente-de-prueba",
                "GOOGLE_CLIENT_SECRET=secreto-de-prueba"
        }
)
class OAuth2LoginIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("Redirige a Google y guarda el estado en una cookie")
    void redirectsToGoogleAndStoresStateInCookie() throws Exception {
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        HttpRequest request = HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/oauth2/authorization/google")).GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(302, response.statusCode());
        String location = response.headers().firstValue("location").orElseThrow();
        assertTrue(location.startsWith("https://accounts.google.com/o/oauth2/v2/auth"));
        assertTrue(location.contains("client_id=cliente-de-prueba"));
        assertTrue(location.contains("redirect_uri=http"));
        assertTrue(location.contains("login/oauth2/code/google"));
        assertTrue(location.contains("scope=openid"));
        assertTrue(response.headers().allValues("set-cookie").stream()
                .anyMatch(cookie -> cookie.startsWith("pardrops_oauth2_request=")));
    }

    @Test
    @DisplayName("El callback de Google es público y no devuelve 401")
    void callbackIsPublic() throws Exception {
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        HttpRequest request = HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/login/oauth2/code/google")).GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(302, response.statusCode());
        assertTrue(response.headers().firstValue("location").orElse("")
                .startsWith("http://localhost:4200/login?oauth="));
    }
}
