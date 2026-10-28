package com.pardorps.pardrops.dto;

/**
 * Resultado de renderizar una plantilla.
 *
 * @param subject asunto final.
 * @param bodyHtml cuerpo HTML final.
 */
public record RenderedEmail(String subject, String bodyHtml) {
}
