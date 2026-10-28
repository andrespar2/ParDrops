package com.pardorps.pardrops.dto;

import com.pardorps.pardrops.model.Product;

import java.util.List;

/**
 * Rankings de la portada.
 *
 * @param bestSellers productos con más unidades vendidas.
 * @param topRated    productos con mejor valoración.
 */
public record ProductHighlightsResponse(
        List<Product> bestSellers,
        List<Product> topRated
) {
}
