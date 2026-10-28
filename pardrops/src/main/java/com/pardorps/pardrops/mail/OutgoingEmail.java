package com.pardorps.pardrops.mail;

/**
 * Correo listo para entregar.
 *
 * @param to      destinatario.
 * @param subject asunto.
 * @param html    cuerpo HTML.
 */
public record OutgoingEmail(String to, String subject, String html) {
}
