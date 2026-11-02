package com.pardorps.pardrops.dto;

import java.time.Instant;

/**
 * Mensaje de una conversación de soporte.
 *
 * @param id         identificador.
 * @param body       contenido.
 * @param fromStaff  lo escribió el equipo de soporte.
 * @param authorName nombre visible del autor.
 * @param createdAt  fecha de envío.
 */
public record SupportMessageResponse(
        Long id,
        String body,
        boolean fromStaff,
        String authorName,
        Instant createdAt
) {
}
