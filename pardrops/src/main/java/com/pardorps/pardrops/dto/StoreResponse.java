package com.pardorps.pardrops.dto;

import com.pardorps.pardrops.model.StoreStatus;

/**
 * Representación pública de la tienda de un vendedor.
 *
 * @param id           identificador de la tienda.
 * @param name         nombre comercial.
 * @param slug         slug público de la tienda.
 * @param description  descripción comercial.
 * @param logoUrl      logotipo de la tienda.
 * @param bannerUrl    portada de la tienda.
 * @param contactEmail correo de contacto.
 * @param contactPhone teléfono de contacto.
 * @param whatsapp     WhatsApp de contacto.
 * @param status       estado de la tienda.
 * @param sellerName   nombre del vendedor.
 * @param productCount número de productos publicados en la tienda.
 * @param createdAt    fecha de alta de la tienda.
 */
public record StoreResponse(
        Long id,
        String name,
        String slug,
        String description,
        String logoUrl,
        String bannerUrl,
        String contactEmail,
        String contactPhone,
        String whatsapp,
        StoreStatus status,
        String sellerName,
        long productCount,
        java.time.Instant createdAt
) {
}
