package com.pardorps.pardrops.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Datos que el administrador envía para crear o editar una categoría.
 *
 * @param name        nombre de la categoría (obligatorio).
 * @param slug        slug único para la URL (opcional; se genera del nombre).
 * @param description descripción de la categoría (opcional).
 * @param active      indica si la categoría está disponible.
 * @param featured    indica si se muestra en el menú del catálogo.
 * @param parentId    categoría padre (opcional, para subcategorías).
 * @param sortOrder   orden de aparición.
 * @param imageUrl    imagen representativa.
 * @param collection  indica si es una colección (ofertas, temporadas...).
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

        Boolean featured,

        Long parentId,

        @PositiveOrZero(message = "El orden no puede ser negativo")
        Integer sortOrder,

        @Size(max = 500, message = "La URL de la imagen es demasiado larga")
        String imageUrl,

        Boolean collection
) {

    /**
     * Constructor compatible con las altas que aún no envían jerarquía.
     */
    public CategoryRequest(
            String name,
            String slug,
            String description,
            Boolean active,
            Boolean featured
    ) {
        this(name, slug, description, active, featured, null, null, null, null);
    }
}
