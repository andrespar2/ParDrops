package com.pardorps.pardrops.mail;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.UUID;

/**
 * Guarda los correos en disco cuando no hay SMTP configurado.
 *
 * <p>Escribe un archivo {@code .eml} (el mensaje completo) y un {@code .html}
 * (el cuerpo) para poder revisarlos sin credenciales ni conexión.</p>
 */
@Component
@ConditionalOnProperty(
        name = "app.mail.enabled",
        havingValue = "false",
        matchIfMissing = true)
public class OutboxMailGateway implements MailGateway {

    private static final Logger log = LoggerFactory.getLogger(OutboxMailGateway.class);

    private static final DateTimeFormatter FILE_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private final Path directory;
    private final String from;
    private final String fromName;

    /**
     * @param directory ruta del outbox.
     * @param from      dirección remitente.
     * @param fromName  nombre visible.
     */
    public OutboxMailGateway(
            @Value("${app.mail.outbox-dir:./data/mail-outbox}") String directory,
            @Value("${app.mail.from:no-reply@pardrops.local}") String from,
            @Value("${app.mail.from-name:ParDrops}") String fromName
    ) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
        this.from = from;
        this.fromName = fromName;
        try {
            Files.createDirectories(this.directory);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo crear el outbox de correo", ex);
        }
    }

    /**
     * Escribe el correo en el outbox.
     *
     * @param email correo a guardar.
     * @throws MailDeliveryException si no se puede escribir.
     */
    @Override
    public void send(OutgoingEmail email) {
        String baseName = FILE_DATE.format(LocalDateTime.now())
                + "-" + safe(email.to())
                + "-" + UUID.randomUUID().toString().substring(0, 8);
        try {
            MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(from, fromName, StandardCharsets.UTF_8.name()));
            helper.setTo(email.to());
            helper.setSubject(email.subject());
            helper.setText(email.html(), true);

            try (OutputStream output = Files.newOutputStream(directory.resolve(baseName + ".eml"))) {
                message.writeTo(output);
            }
            Files.writeString(
                    directory.resolve(baseName + ".html"), email.html(), StandardCharsets.UTF_8);
            log.info("Correo guardado en el outbox: {}", baseName);
        } catch (Exception ex) {
            throw new MailDeliveryException(
                    "No se pudo guardar el correo en el outbox", ex);
        }
    }

    /** Limpia el destinatario para usarlo como nombre de archivo. */
    private String safe(String value) {
        String safe = value == null ? "sin-destinatario" : value.replaceAll("[^a-zA-Z0-9._-]", "_");
        return safe.length() > 60 ? safe.substring(0, 60) : safe;
    }
}
