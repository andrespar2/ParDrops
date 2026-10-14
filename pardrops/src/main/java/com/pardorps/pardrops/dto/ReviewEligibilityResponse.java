package com.pardorps.pardrops.dto;

/**
 * Indica si el usuario actual puede dejar una reseña en un producto.
 *
 * <p>Lo usa el frontend para decidir si muestra el formulario, un aviso de
 * "ya has valorado" o el mensaje pidiendo iniciar sesión.</p>
 *
 * @param canReview   {@code true} si puede publicar la reseña ahora mismo.
 * @param hasReviewed {@code true} si el usuario ya reseñó el producto.
 */
public record ReviewEligibilityResponse(
        boolean canReview,
        boolean hasReviewed
) {
}
