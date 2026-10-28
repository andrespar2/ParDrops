package com.pardorps.pardrops.service;

import com.pardorps.pardrops.mail.MailGateway;
import com.pardorps.pardrops.mail.OutgoingEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Envía correos transaccionales sin romper el flujo que los origina.
 *
 * <p>Cualquier fallo de entrega se registra y se absorbe: un problema de correo
 * no debe revertir un pedido ni un cambio de estado.</p>
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

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
     * @return {@code true} si la entrega no lanzó error.
     */
    public boolean send(String to, String subject, String html) {
        if (to == null || to.isBlank()) {
            log.warn("Correo sin destinatario: {}", subject);
            return false;
        }
        try {
            mailGateway.send(new OutgoingEmail(to.trim(), subject, html));
            log.info("Correo enviado a {}: {}", to, subject);
            return true;
        } catch (Exception ex) {
            log.error("No se pudo enviar el correo a {}: {}", to, ex.getMessage());
            return false;
        }
    }
}
