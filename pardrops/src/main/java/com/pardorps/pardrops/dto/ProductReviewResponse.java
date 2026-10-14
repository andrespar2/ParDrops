package com.pardorps.pardrops.dto;

import java.time.Instant;

/**
 * Representación de una reseña devuelta por la API.
 *
 * <p>Solo expone el nombre visible del autor; el correo y el identificador de
 * la cuenta nunca salen del servidor.</p>
 *
 * @param id           identificador de la reseña.
 * @param rating       calificación de 1 a 5 estrellas.
 * @param comment      comentario escrito por el cliente.
 * @param customerName nombre completo del autor.
 * @param createdAt    fecha de publicación.
 */
public record ProductReviewResponse(
        Long id,
        int rating,
        String comment,
        String customerName,
        Instant createdAt
) {
}
