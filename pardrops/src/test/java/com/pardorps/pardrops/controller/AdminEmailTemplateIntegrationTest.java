package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.model.EmailTemplateCode;
import com.pardorps.pardrops.repository.EmailTemplateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de integración del editor de plantillas del panel.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminEmailTemplateIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private EmailTemplateRepository repository;

    @Test
    @DisplayName("El administrador ve las tres plantillas")
    void adminListsTemplates() {
        RestClient client = client();
        String token = adminToken(client);

        String body = client.get().uri("/api/admin/email-templates")
                .header("Authorization", "Bearer " + token)
                .retrieve().body(String.class);

        assertTrue(body.contains("SELLER_NEW_ORDER"));
        assertTrue(body.contains("BUYER_PAYMENT_RECEIPT"));
        assertTrue(body.contains("BUYER_ORDER_STATUS"));
    }

    @Test
    @DisplayName("El administrador actualiza y restaura una plantilla")
    void adminUpdatesAndResetsTemplate() {
        RestClient client = client();
        String token = adminToken(client);

        String updated = client.put()
                .uri("/api/admin/email-templates/BUYER_ORDER_STATUS")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("subject", "Asunto temporal", "bodyHtml", "<p>Hola</p>", "active", true))
                .retrieve().body(String.class);
        assertTrue(updated.contains("Asunto temporal"));

        String reset = client.post()
                .uri("/api/admin/email-templates/BUYER_ORDER_STATUS/reset")
                .header("Authorization", "Bearer " + token)
                .retrieve().body(String.class);
        assertTrue(reset.contains("Tu pedido #{{orderId}} está {{statusLabel}}"));
    }

    @Test
    @DisplayName("La vista previa devuelve el HTML con datos de ejemplo")
    void adminPreviewsTemplate() {
        RestClient client = client();
        String token = adminToken(client);

        String body = client.post()
                .uri("/api/admin/email-templates/SELLER_NEW_ORDER/preview")
                .header("Authorization", "Bearer " + token)
                .retrieve().body(String.class);

        assertTrue(body.contains("subject"));
        assertTrue(body.contains("bodyHtml"));
        assertTrue(body.contains("Nuevo pedido #1024"));
        assertFalse(body.contains("{{"));
    }

    @Test
    @DisplayName("Un cliente no puede acceder al editor")
    void customerIsForbidden() {
        RestClient client = client();
        String token = customerToken(client);

        HttpStatus status = client.get().uri("/api/admin/email-templates")
                .header("Authorization", "Bearer " + token)
                .exchange((request, response) -> HttpStatus.valueOf(response.getStatusCode().value()));

        assertEquals(HttpStatus.FORBIDDEN, status);
    }

    private RestClient client() {
        return RestClient.builder().baseUrl("http://localhost:" + port).build();
    }

    private String adminToken(RestClient client) {
        return token(client, "admin@pardrops.com", "admin123");
    }

    private String customerToken(RestClient client) {
        String email = "cliente-plantillas-" + System.nanoTime() + "@ejemplo.com";
        client.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("fullName", "Cliente Plantillas", "email", email, "password", "clave123"))
                .retrieve().body(String.class);
        return token(client, email, "clave123");
    }

    private String token(RestClient client, String email, String password) {
        String body = client.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", email, "password", password))
                .retrieve().body(String.class);
        int index = body.indexOf("\"token\":\"") + 9;
        return body.substring(index, body.indexOf('"', index));
    }
}
