package com.pardorps.pardrops.payment;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

/**
 * Cliente HTTP de la API de Wompi.
 *
 * <p>Usa {@code java.net.http.HttpClient} para no añadir dependencias y
 * autentica con la llave privada del ambiente activo. Los mensajes de error
 * nunca incluyen la llave ni el cuerpo completo de la respuesta.</p>
 */
@Component
public class HttpWompiClient implements WompiClient {

    private static final Logger log = LoggerFactory.getLogger(HttpWompiClient.class);

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final WompiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    /**
     * @param properties   configuración y llaves de Wompi.
     * @param objectMapper lector de JSON.
     */
    public HttpWompiClient(WompiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    @Override
    public WompiTransaction fetchTransaction(String transactionId) {
        if (!properties.isVerificationConfigured()) {
            throw new WompiClientException("Wompi no tiene llave privada configurada", 0);
        }
        if (transactionId == null || transactionId.isBlank()) {
            throw new WompiClientException("Identificador de transacción vacío", 0);
        }

        String url = properties.apiBaseUrl() + "/transactions/"
                + java.net.URLEncoder.encode(transactionId, java.nio.charset.StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + properties.privateKey())
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Wompi rechazó la consulta de la transacción {}: HTTP {}",
                        mask(transactionId), response.statusCode());
                throw new WompiClientException(
                        "Wompi respondió HTTP " + response.statusCode(), response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.path("data");
            if (data.isMissingNode() || data.isNull()) {
                throw new WompiClientException("Respuesta de Wompi sin datos", 200);
            }
            return new WompiTransaction(
                    text(data, "id"),
                    text(data, "reference"),
                    text(data, "status").toUpperCase(java.util.Locale.ROOT),
                    data.path("amount_in_cents").asLong(0L),
                    text(data, "currency"),
                    text(data, "payment_method_type"),
                    text(data, "customer_email"),
                    text(data, "status_message"),
                    instant(data.path("created_at").asText(null))
            );
        } catch (WompiClientException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new WompiClientException("Consulta a Wompi interrumpida", exception);
        } catch (Exception exception) {
            log.warn("No se pudo consultar la transacción {} en Wompi: {}",
                    mask(transactionId), exception.getMessage());
            throw new WompiClientException("No se pudo consultar Wompi", exception);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }

    private Instant instant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception exception) {
            return null;
        }
    }

    /** Muestra solo el final del identificador para no ensuciar los logs. */
    private String mask(String value) {
        if (value == null || value.length() <= 6) {
            return "***";
        }
        return "***" + value.substring(value.length() - 6);
    }
}
