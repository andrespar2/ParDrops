package com.pardorps.pardrops.mail;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Envía los correos por SMTP.
 *
 * <p>Solo se activa con {@code app.mail.enabled=true}; en desarrollo se usa el
 * outbox.</p>
 */
@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class SmtpMailGateway implements MailGateway {

    private final JavaMailSender mailSender;
    private final String from;
    private final String fromName;

    /**
     * @param mailSender remitente de Spring Boot.
     * @param from       dirección remitente.
     * @param fromName   nombre visible.
     */
    public SmtpMailGateway(
            JavaMailSender mailSender,
            @Value("${app.mail.from:no-reply@pardrops.local}") String from,
            @Value("${app.mail.from-name:ParDrops}") String fromName
    ) {
        this.mailSender = mailSender;
        this.from = from;
        this.fromName = fromName;
    }

    /**
     * Compone y envía el mensaje.
     *
     * @param email correo a enviar.
     * @throws MailDeliveryException si JavaMail falla.
     */
    @Override
    public void send(OutgoingEmail email) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(from, fromName, StandardCharsets.UTF_8.name()));
            helper.setTo(email.to());
            helper.setSubject(email.subject());
            helper.setText(email.html(), true);
            mailSender.send(message);
        } catch (Exception ex) {
            throw new MailDeliveryException(
                    "No se pudo enviar el correo a " + email.to(), ex);
        }
    }
}
