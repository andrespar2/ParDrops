package com.pardorps.pardrops.dto;

/**
 * Representación de una línea de pedido devuelta por la API.
 *
 * @param productId   producto comprado.
 * @param productName nombre del producto en el momento de la compra.
 * @param productSlug slug del producto.
 * @param variantName variante elegida (o nulo).
 * @param unitPrice   precio unitario aplicado.
 * @param quantity    unidades compradas.
 * @param subtotal    importe de la línea.
 */
public record OrderItemResponse(
        String productId,
        String productName,
        String productSlug,
        String variantName,
        Double unitPrice,
        Integer quantity,
        Double subtotal
) {
}
