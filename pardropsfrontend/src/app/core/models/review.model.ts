/**
 * @file src/app/core/models/review.model.ts
 * @description Modelos relacionados con las reseñas de productos.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

/**
 * Reseña de un producto tal y como la devuelve la API.
 *
 * @interface ProductReview
 */
export interface ProductReview {
  /** Identificador de la reseña. */
  id: number;
  /** Calificación otorgada, de 1 a 5 estrellas. */
  rating: number;
  /** Comentario escrito por el cliente. */
  comment: string;
  /** Nombre visible del autor de la reseña. */
  customerName: string;
  /** Fecha de publicación (ISO 8601). */
  createdAt: string;
}

/**
 * Estado de elegibilidad del usuario actual para reseñar un producto.
 *
 * @interface ReviewEligibility
 */
export interface ReviewEligibility {
  /** Indica si el usuario puede publicar una reseña ahora mismo. */
  canReview: boolean;
  /** Indica si el usuario ya reseñó el producto. */
  hasReviewed: boolean;
}

/**
 * Datos enviados al publicar una reseña.
 *
 * @interface CreateReviewRequest
 */
export interface CreateReviewRequest {
  /** Calificación de 1 a 5 estrellas. */
  rating: number;
  /** Comentario sobre la experiencia. */
  comment: string;
}
