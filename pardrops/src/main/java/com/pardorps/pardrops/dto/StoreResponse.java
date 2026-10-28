package com.pardorps.pardrops.dto;

import com.pardorps.pardrops.model.StoreStatus;

/**
 * Representación pública de la tienda de un vendedor.
 *
 * <p>No incluye el nombre, el correo ni el teléfono personal del vendedor.
 * Solo expone los datos comerciales que el comprador necesita.</p>
 *
 * @param id           identificador de la tienda.
 * @param name         nombre comercial.
 * @param slug         slug público de la tienda.
 * @param description  descripción comercial.
 * @param logoUrl      logotipo de la tienda.
 * @param bannerUrl    portada de la tienda.
 * @param publicEmail  correo público de la tienda.
 * @param publicPhone  teléfono público de la tienda.
 * @param city         ciudad de la tienda.
 * @param whatsapp     WhatsApp de contacto.
 * @param status       estado de la tienda.
 * @param rating       media de estrellas de sus productos.
 * @param reviewCount  número de reseñas de sus productos.
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
        String publicEmail,
        String publicPhone,
        String city,
        String whatsapp,
        StoreStatus status,
        double rating,
        long reviewCount,
        long productCount,
        java.time.Instant createdAt
) {
}
