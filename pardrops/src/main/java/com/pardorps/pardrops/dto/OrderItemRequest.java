package com.pardorps.pardrops.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Línea de producto incluida en un pedido.
 *
 * <p>Solo se reciben la referencia del producto, la variante y la cantidad.
 * El precio se calcula en el servidor a partir del catálogo para evitar que el
 * cliente pueda manipularlo.</p>
 *
 * @param productId   identificador del producto comprado.
 * @param productSlug slug del producto (útil para enlazar desde el historial).
 * @param variantId   identificador estable de la variante (opcional).
 * @param variantName nombre de la variante elegida (opcional).
 * @param quantity    unidades compradas (mínimo 1).
 */
public record OrderItemRequest(

        @NotBlank(message = "El producto es obligatorio")
        String productId,

        String productSlug,

        String variantId,

        String variantName,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad mínima es 1")
        Integer quantity
) {

    /**
     * Constructor compatible con las peticiones antiguas que solo envían el
     * nombre de la variante.
     *
     * @param productId   producto.
     * @param productSlug slug.
     * @param variantName nombre de la variante.
     * @param quantity    cantidad.
     */
    public OrderItemRequest(String productId, String productSlug, String variantName, Integer quantity) {
        this(productId, productSlug, null, variantName, quantity);
    }
}
