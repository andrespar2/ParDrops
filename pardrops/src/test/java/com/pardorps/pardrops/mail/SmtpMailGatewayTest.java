package com.pardorps.pardrops.mail;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas del gateway SMTP.
 */
class SmtpMailGatewayTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);

    @Test
    @DisplayName("Compone y envía el mensaje por JavaMail")
    void sendsMessage() {
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        SmtpMailGateway gateway = new SmtpMailGateway(
                mailSender, "no-reply@pardrops.test", "ParDrops");

        gateway.send(new OutgoingEmail("cliente@pardrops.com", "Asunto", "<p>Hola</p>"));

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Envuelve el fallo de JavaMail en un error controlado")
    void wrapsJavaMailFailure() {
        when(mailSender.createMimeMessage())
                .thenThrow(new MailSendException("sin conexión"));
        SmtpMailGateway gateway = new SmtpMailGateway(
                mailSender, "no-reply@pardrops.test", "ParDrops");

        assertThrows(MailDeliveryException.class,
                () -> gateway.send(new OutgoingEmail("cliente@pardrops.com", "Asunto", "<p>Hola</p>")));
    }
}
