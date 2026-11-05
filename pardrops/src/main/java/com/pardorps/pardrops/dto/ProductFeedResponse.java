package com.pardorps.pardrops.dto;

import com.pardorps.pardrops.model.Product;

import java.util.List;

/**
 * Página del catálogo con la semilla de distribución utilizada.
 *
 * <p>El cliente debe reenviar la misma semilla al pedir páginas siguientes
 * para conservar el orden. Una recarga completa genera una semilla nueva.</p>
 *
 * @param content       productos de la página.
 * @param seed          semilla de la distribución.
 * @param page          página actual (0-based).
 * @param size          tamaño de página.
 * @param totalElements total de productos que cumplen los filtros.
 * @param totalPages    total de páginas.
 */
public record ProductFeedResponse(
        List<Product> content,
        String seed,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
