package com.pardorps.pardrops.dto;

import com.pardorps.pardrops.model.EmailTemplateCode;

import java.time.Instant;

/**
 * Representación de una plantilla de correo para el panel.
 *
 * @param id          identificador.
 * @param code        código del correo.
 * @param name        nombre visible.
 * @param description explicación de uso.
 * @param subject     asunto.
 * @param bodyHtml    cuerpo HTML.
 * @param active      si está activa.
 * @param updatedAt   última modificación.
 */
public record EmailTemplateResponse(
        Long id,
        EmailTemplateCode code,
        String name,
        String description,
        String subject,
        String bodyHtml,
        boolean active,
        Instant updatedAt
) {
}
