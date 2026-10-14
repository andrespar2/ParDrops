package com.pardorps.pardrops.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos que un cliente envía para publicar una reseña de producto.
 *
 * <p>El autor no se envía en el cuerpo: siempre se toma de la sesión JWT, de
 * modo que nadie pueda reseñar en nombre de otra persona.</p>
 *
 * @param rating  calificación de 1 a 5 estrellas.
 * @param comment comentario escrito sobre la experiencia.
 */
public record CreateReviewRequest(

        @NotNull(message = "La calificación es obligatoria")
        @Min(value = 1, message = "La calificación mínima es 1 estrella")
        @Max(value = 5, message = "La calificación máxima es 5 estrellas")
        Integer rating,

        @NotBlank(message = "El comentario es obligatorio")
        @Size(max = 2000, message = "El comentario es demasiado largo")
        String comment
) {
}
