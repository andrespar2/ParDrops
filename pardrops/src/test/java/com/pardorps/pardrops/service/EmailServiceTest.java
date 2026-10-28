package com.pardorps.pardrops.service;

import com.pardorps.pardrops.mail.MailDeliveryException;
import com.pardorps.pardrops.mail.MailGateway;
import com.pardorps.pardrops.mail.OutgoingEmail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Pruebas del servicio de envío de correo.
 */
class EmailServiceTest {

    private final MailGateway gateway = mock(MailGateway.class);
    private final EmailService service = new EmailService(gateway);

    @Test
    @DisplayName("Entrega el correo al gateway")
    void sendsEmail() {
        assertTrue(service.send("cliente@pardrops.com", "Asunto", "<p>Hola</p>"));

        verify(gateway).send(new OutgoingEmail(
                "cliente@pardrops.com", "Asunto", "<p>Hola</p>"));
    }

    @Test
    @DisplayName("Un fallo de entrega se registra y no se propaga")
    void absorbsDeliveryFailure() {
        doThrow(new MailDeliveryException("SMTP caído", null))
                .when(gateway).send(any(OutgoingEmail.class));

        assertFalse(service.send("cliente@pardrops.com", "Asunto", "<p>Hola</p>"));
    }

    @Test
    @DisplayName("No intenta enviar sin destinatario")
    void skipsEmptyRecipient() {
        assertFalse(service.send("  ", "Asunto", "<p>Hola</p>"));

        verify(gateway, never()).send(any(OutgoingEmail.class));
    }
}
