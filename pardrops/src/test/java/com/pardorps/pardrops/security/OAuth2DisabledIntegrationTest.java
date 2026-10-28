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
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Comprueba que el acceso con Google queda inactivo cuando no hay credenciales.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "GOOGLE_CLIENT_ID="
)
class OAuth2DisabledIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("Sin credenciales el inicio del flujo de Google no existe")
    void googleFlowIsDisabledWithoutCredentials() throws Exception {
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        HttpRequest request = HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/oauth2/authorization/google")).GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
        assertFalse(response.headers().firstValue("location").orElse("")
                .contains("accounts.google.com"));
    }
}
