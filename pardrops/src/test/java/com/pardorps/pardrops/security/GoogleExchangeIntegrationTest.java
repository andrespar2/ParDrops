package com.pardorps.pardrops.security;

import com.pardorps.pardrops.model.AuthProvider;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.UserRepository;
import com.pardorps.pardrops.service.GoogleLoginCodeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del canje del código temporal por la sesión JWT.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GoogleExchangeIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GoogleLoginCodeService codeService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("El código válido devuelve el JWT y los datos de la cuenta")
    void validCodeReturnsJwt() throws Exception {
        User user = crearCuentaGoogle();
        String code = codeService.issue(user.getId());

        HttpResponse<String> response = canjear(code);

        assertEquals(200, response.statusCode());
        JsonNode json = objectMapper.readTree(response.body());
        assertFalse(json.get("token").asString().isBlank());
        assertEquals(user.getEmail(), json.get("user").get("email").asString());
        assertEquals("GOOGLE", json.get("user").get("provider").asString());
    }

    @Test
    @DisplayName("El código no se puede canjear dos veces")
    void codeCannotBeExchangedTwice() throws Exception {
        User user = crearCuentaGoogle();
        String code = codeService.issue(user.getId());

        assertEquals(200, canjear(code).statusCode());
        assertEquals(401, canjear(code).statusCode());
    }

    @Test
    @DisplayName("Un código inventado devuelve 401")
    void unknownCodeIsRejected() throws Exception {
        assertEquals(401, canjear("codigo-inventado").statusCode());
    }

    private HttpResponse<String> canjear(String code) throws Exception {
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        String body = objectMapper.writeValueAsString(java.util.Map.of("code", code));
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/auth/google/exchange"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private User crearCuentaGoogle() {
        String suffix = UUID.randomUUID().toString();
        return userRepository.save(User.builder()
                .email("google-" + suffix + "@pardrops.com")
                .password("hash-tecnico")
                .fullName("Cuenta Google")
                .role(Role.CUSTOMER)
                .authProvider(AuthProvider.GOOGLE)
                .providerId("sub-" + suffix)
                .avatarUrl("https://foto.example/" + suffix)
                .build());
    }
}
