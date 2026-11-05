package com.pardorps.pardrops.dto;

import com.pardorps.pardrops.model.StoreStatus;

/**
 * Resumen de una tienda para el administrador de productos.
 *
 * @param storeId        identificador de la tienda (nulo para el catálogo propio).
 * @param sellerId       identificador del vendedor (nulo para el catálogo propio).
 * @param storeName      nombre comercial.
 * @param storeSlug      slug público.
 * @param sellerName     nombre del vendedor.
 * @param status         estado de la tienda.
 * @param productCount   total de productos.
 * @param publishedCount productos publicados.
 * @param hiddenCount    productos ocultos.
 */
public record AdminStoreProductSummary(
        Long storeId,
        Long sellerId,
        String storeName,
        String storeSlug,
        String sellerName,
        StoreStatus status,
        long productCount,
        long publishedCount,
        long hiddenCount
) {
}
