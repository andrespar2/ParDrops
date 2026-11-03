package com.pardorps.pardrops.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Envía correos con la API HTTPS de Brevo.
 *
 * <p>Se usa cuando el proveedor bloquea los puertos SMTP (Render bloquea el 587
 * en instancias gratuitas). La API viaja por HTTPS (443) y el mensaje mantiene
 * el mismo contenido HTML.</p>
 */
@Component
@ConditionalOnProperty(name = "app.mail.transport", havingValue = "api")
public class BrevoApiMailGateway implements MailGateway {

    private static final Logger log = LoggerFactory.getLogger(BrevoApiMailGateway.class);

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final String apiKey;
    private final String from;
    private final String fromName;

    /**
     * @param apiKey   clave de API de Brevo.
     * @param from     remitente verificado.
     * @param fromName nombre visible del remitente.
     */
    public BrevoApiMailGateway(
            @Value("${app.mail.brevo-api-key:}") String apiKey,
            @Value("${app.mail.from:no-reply@pardrops.local}") String from,
            @Value("${app.mail.from-name:ParDrops}") String fromName
    ) {
        this.apiKey = apiKey;
        this.from = from;
        this.fromName = fromName;
    }

    @Override
    public void send(OutgoingEmail email) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new MailDeliveryException("Falta la clave de API de Brevo", null);
        }

        String json = "{"
                + "\"sender\":{\"name\":\"" + escape(fromName) + "\",\"email\":\"" + escape(from) + "\"},"
                + "\"to\":[{\"email\":\"" + escape(email.to()) + "\"}],"
                + "\"subject\":\"" + escape(email.subject()) + "\","
                + "\"htmlContent\":\"" + escape(email.html()) + "\""
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                .timeout(Duration.ofSeconds(20))
                .header("api-key", apiKey)
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Brevo API aceptó el correo para {}", email.to());
                return;
            }
            throw new MailDeliveryException("Brevo API respondió " + response.statusCode()
                    + ": " + safe(response.body()), null);
        } catch (MailDeliveryException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new MailDeliveryException("No se pudo enviar por la API de Brevo", exception);
        }
    }

    /** Escapa cadenas para JSON. */
    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /** Recorta la respuesta para el registro. */
    private String safe(String body) {
        if (body == null) {
            return "";
        }
        return body.length() > 300 ? body.substring(0, 300) : body;
    }
}
