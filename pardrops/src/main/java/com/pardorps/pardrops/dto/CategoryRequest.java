package com.pardorps.pardrops.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos que el administrador envía para crear o editar una categoría.
 *
 * @param name        nombre de la categoría (obligatorio).
 * @param slug        slug único para la URL (opcional; se genera del nombre).
 * @param description descripción de la categoría (opcional).
 * @param active      indica si la categoría está disponible.
 * @param featured    indica si se muestra en el menú del catálogo.
 */
public record CategoryRequest(

        @NotBlank(message = "El nombre de la categoría es obligatorio")
        @Size(max = 120, message = "El nombre de la categoría es demasiado largo")
        String name,

        @Size(max = 140, message = "El slug de la categoría es demasiado largo")
        String slug,

        @Size(max = 500, message = "La descripción es demasiado larga")
        String description,

        Boolean active,

        Boolean featured
) {
}
