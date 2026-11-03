package com.pardorps.pardrops.service;

import com.pardorps.pardrops.mail.MailGateway;
import com.pardorps.pardrops.mail.OutgoingEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Envía correos transaccionales con reintentos y control de duplicados.
 *
 * <p>Un fallo de correo nunca revierte la operación que lo origina: el error se
 * registra y la notificación interna se conserva. Los envíos repetidos del
 * mismo mensaje dentro de la ventana de deduplicación se ignoran.</p>
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    /** Intentos totales por mensaje. */
    private static final int MAX_ATTEMPTS = 3;

    /** Ventana de deduplicación en milisegundos (10 minutos). */
    private static final long DEDUPE_WINDOW_MS = 10 * 60 * 1000L;

    /** Mensajes enviados recientemente: clave -> instante. */
    private final Map<String, Instant> recent = new ConcurrentHashMap<>();

    private final MailGateway mailGateway;

    /**
     * @param mailGateway entrega de correos.
     */
    public EmailService(MailGateway mailGateway) {
        this.mailGateway = mailGateway;
    }

    /**
     * Envía un correo HTML.
     *
     * @param to      destinatario.
     * @param subject asunto.
     * @param html    cuerpo HTML.
     * @return {@code true} si la entrega se completó.
     */
    public boolean send(String to, String subject, String html) {
        if (to == null || to.isBlank()) {
            log.warn("Correo sin destinatario: {}", subject);
            return false;
        }
        String recipient = to.trim();
        String key = key(recipient, subject, html);
        cleanup();

        if (recent.containsKey(key)) {
            log.info("Correo duplicado omitido para {}: {}", recipient, subject);
            return true;
        }

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                mailGateway.send(new OutgoingEmail(recipient, subject, html));
                recent.put(key, Instant.now());
                log.info("Correo enviado (intento {}) a {}: {}", attempt, recipient, subject);
                return true;
            } catch (Exception ex) {
                log.warn("Fallo de correo (intento {}/{}) a {}: {}",
                        attempt, MAX_ATTEMPTS, recipient, ex.getMessage());
                if (attempt < MAX_ATTEMPTS) {
                    sleep(400L * attempt);
                }
            }
        }

        log.error("Correo no entregado tras {} intentos a {}: {}", MAX_ATTEMPTS, recipient, subject);
        return false;
    }

    /** Clave de deduplicación del mensaje. */
    private String key(String to, String subject, String html) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((to + "|" + subject + "|" + html).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            return to + "|" + subject;
        }
    }

    /** Elimina claves antiguas para no crecer sin límite. */
    private void cleanup() {
        Instant limit = Instant.now().minusMillis(DEDUPE_WINDOW_MS);
        recent.entrySet().removeIf(entry -> entry.getValue().isBefore(limit));
    }

    /** Espera entre reintentos. */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
