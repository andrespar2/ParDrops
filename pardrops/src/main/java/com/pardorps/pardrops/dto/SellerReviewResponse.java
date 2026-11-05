package com.pardorps.pardrops.dto;

import java.time.Instant;
import java.util.List;

/**
 * Reseña visible para el vendedor propietario del producto.
 *
 * <p>Es un contrato de solo lectura: el vendedor puede ver la valoración y el
 * comentario, pero no existe ningún endpoint que le permita modificarlos.</p>
 *
 * @param id          identificador de la reseña.
 * @param productId   producto reseñado.
 * @param productName nombre del producto.
 * @param rating      estrellas (1 a 5).
 * @param comment     comentario del comprador.
 * @param authorName  nombre del autor (nunca su correo).
 * @param images      imágenes adjuntas.
 * @param createdAt   fecha de publicación.
 */
public record SellerReviewResponse(
        Long id,
        String productId,
        String productName,
        int rating,
        String comment,
        String authorName,
        List<String> images,
        Instant createdAt
) {
}
