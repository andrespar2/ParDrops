package com.pardorps.pardrops.dto;

import com.pardorps.pardrops.model.StoreStatus;
import com.pardorps.pardrops.model.UserStatus;

import java.time.Instant;

/**
 * Ficha de un producto para el panel de administración.
 *
 * <p>Además de los datos del producto incluye el propietario (vendedor y su
 * tienda) y el estado de visibilidad, que es lo que el administrador necesita
 * para moderar el catálogo global del marketplace.</p>
 *
 * @param id          identificador del producto.
 * @param name        nombre visible.
 * @param slug        slug del producto.
 * @param description descripción corta.
 * @param price       precio actual.
 * @param currency    código de moneda.
 * @param stock       unidades disponibles.
 * @param active      indica si está publicado en el catálogo.
 * @param categories  categorías del producto.
 * @param imageUrl    URL de la imagen principal (nulo si no tiene imágenes).
 * @param sellerId    identificador del vendedor (nulo si es de la plataforma).
 * @param sellerName  nombre del vendedor (nulo si es de la plataforma).
 * @param sellerEmail correo del vendedor (nulo si es de la plataforma).
 * @param storeId     identificador de la tienda del vendedor.
 * @param storeName   nombre de la tienda del vendedor.
 * @param storeSlug   slug de la tienda del vendedor.
 * @param storeStatus estado de la tienda del vendedor.
 * @param sellerStatus estado de la cuenta del vendedor.
 * @param createdAt   fecha de alta del producto.
 * @param updatedAt   fecha de la última modificación.
 */
public record AdminProductResponse(
        String id,
        String name,
        String slug,
        String description,
        Double price,
        String currency,
        int stock,
        boolean active,
        java.util.List<String> categories,
        String imageUrl,
        Long sellerId,
        String sellerName,
        String sellerEmail,
        Long storeId,
        String storeName,
        String storeSlug,
        StoreStatus storeStatus,
        UserStatus sellerStatus,
        Instant createdAt,
        Instant updatedAt
) {
}
