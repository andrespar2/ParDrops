package com.pardorps.pardrops.mail;

/**
 * Entrega un correo. Permite cambiar SMTP por outbox o por una cola sin tocar
 * los servicios que generan los mensajes.
 */
public interface MailGateway {

    /**
     * Envía un correo.
     *
     * @param email mensaje a enviar.
     * @throws MailDeliveryException si no se puede entregar.
     */
    void send(OutgoingEmail email);
}
