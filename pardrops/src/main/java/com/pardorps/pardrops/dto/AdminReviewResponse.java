package com.pardorps.pardrops.dto;

import java.time.Instant;
import java.util.List;

/**
 * Reseña vista por el administrador para su moderación.
 *
 * @param id           identificador.
 * @param productId    producto reseñado.
 * @param productName  nombre del producto.
 * @param rating       estrellas.
 * @param comment      comentario.
 * @param customerName autor.
 * @param images       imágenes adjuntas.
 * @param createdAt    fecha.
 */
public record AdminReviewResponse(
        Long id,
        String productId,
        String productName,
        int rating,
        String comment,
        String customerName,
        List<String> images,
        Instant createdAt
) {
}
