package com.pardorps.pardrops.security;

import com.pardorps.pardrops.model.AuthProvider;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.UserRepository;
import com.pardorps.pardrops.service.GoogleAuthService;
import com.pardorps.pardrops.service.GoogleLoginCodeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Recorre el flujo completo del backend: identidad de Google, cuenta, código
 * temporal, JWT y consulta del perfil autenticado.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GoogleAccountFlowIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private GoogleAuthService googleAuthService;

    @Autowired
    private GoogleLoginCodeService codeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Una identidad nueva crea la cuenta y permite consultar el perfil con el JWT")
    void newIdentityCreatesAccountAndReturnsProfile() throws Exception {
        String suffix = UUID.randomUUID().toString();

        User created = googleAuthService.resolveUser(googleIdentity(
                "nueva-" + suffix + "@pardrops.com", "sub-" + suffix, "true"));

        assertEquals(AuthProvider.GOOGLE, created.getAuthProvider());
        assertEquals(Role.CUSTOMER, created.getRole());

        JsonNode session = exchange(codeService.issue(created.getId()));
        assertFalse(session.get("token").asString().isBlank());

        HttpResponse<String> me = get("/api/auth/me", session.get("token").asString());
        assertEquals(200, me.statusCode());
        JsonNode profile = objectMapper.readTree(me.body());
        assertEquals(created.getEmail(), profile.get("email").asString());
        assertEquals("GOOGLE", profile.get("provider").asString());
        assertEquals(created.getAvatarUrl(), profile.get("avatarUrl").asString());
    }

    @Test
    @DisplayName("Una cuenta local se vincula sin duplicarla y conserva su identificador")
    void localAccountIsLinkedWithoutDuplicates() throws Exception {
        String suffix = UUID.randomUUID().toString();
        User local = userRepository.save(User.builder()
                .email("local-" + suffix + "@pardrops.com")
                .password("hash-local")
                .fullName("Nombre elegido")
                .role(Role.CUSTOMER)
                .build());

        User linked = googleAuthService.resolveUser(googleIdentity(
                local.getEmail(), "sub-" + suffix, "true"));

        assertEquals(local.getId(), linked.getId());
        assertEquals(AuthProvider.GOOGLE, linked.getAuthProvider());
        assertEquals("Nombre elegido", linked.getFullName());
        assertTrue(userRepository.findByEmail(local.getEmail()).isPresent());

        JsonNode session = exchange(codeService.issue(linked.getId()));
        assertEquals(local.getId().longValue(), session.get("user").get("id").asLong());
    }

    private JsonNode exchange(String code) throws Exception {
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        String body = objectMapper.writeValueAsString(Map.of("code", code));
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/auth/google/exchange"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        return objectMapper.readTree(response.body());
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private OidcUser googleIdentity(String email, String sub, String verified) {
        OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getClaims()).thenReturn(Map.of(
                "email", email,
                "sub", sub,
                "name", "Nombre de Google",
                "picture", "https://foto.example/" + sub,
                "email_verified", verified
        ));
        return oidcUser;
    }
}
