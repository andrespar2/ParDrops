package com.pardorps.pardrops.dto;

import java.time.Instant;

/**
 * Representación de una categoría del catálogo.
 *
 * <p>Las subcategorías se identifican por {@code parentId}; el cliente arma el
 * árbol con esa relación.</p>
 *
 * @param id          identificador de la categoría.
 * @param name        nombre visible.
 * @param slug        slug de la categoría.
 * @param description descripción de la categoría.
 * @param active      indica si está disponible para asignarla.
 * @param featured    indica si se muestra en el menú del catálogo.
 * @param parentId    identificador de la categoría padre, si es subcategoría.
 * @param parentName  nombre de la categoría padre.
 * @param sortOrder   orden de aparición.
 * @param imageUrl    imagen representativa.
 * @param collection  indica si es una colección (ofertas, temporadas...).
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
        Long parentId,
        String parentName,
        int sortOrder,
        String imageUrl,
        boolean collection,
        long productCount,
        Instant createdAt,
        Instant updatedAt
) {
}
