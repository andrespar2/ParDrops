package com.pardorps.pardrops.dto;

import java.time.Instant;

/**
 * Representación de una categoría del catálogo.
 *
 * @param id          identificador de la categoría.
 * @param name        nombre visible.
 * @param slug        slug de la categoría.
 * @param description descripción de la categoría.
 * @param active      indica si está disponible para asignarla.
 * @param featured    indica si se muestra en el menú del catálogo.
 * @param productCount número de productos que usan la categoría.
 * @param createdAt   fecha de alta.
 * @param updatedAt   fecha de la última modificación.
 */
public record CategoryResponse(
        Long id,
        String name,
        String slug,
        String description,
        boolean active,
        boolean featured,
        long productCount,
        Instant createdAt,
        Instant updatedAt
) {
}
