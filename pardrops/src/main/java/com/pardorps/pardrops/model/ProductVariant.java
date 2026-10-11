package com.pardorps.pardrops.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Variante de un producto (por ejemplo, una talla o un color).
 *
 * <p>Es un tipo embebido porque solo existe dentro de su {@link Product}. Cada
 * variante puede tener su propio precio y stock, que prevalecen sobre los del
 * producto base cuando se selecciona al comprar.</p>
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {

    /** Identificador de la variante (único dentro del producto). */
    @Column(name = "variant_id", nullable = false)
    private String id;

    /** Nombre descriptivo (por ejemplo, "Talla M", "Color Negro"). */
    @Column(name = "variant_name", nullable = false)
    private String name;

    /** Precio específico de la variante; si es nulo se usa el del producto. */
    @Column(name = "variant_price")
    private Double price;

    /** Stock disponible para esta variante; si es nulo se usa el del producto. */
    @Column(name = "variant_stock")
    private Integer stock;

    /** Código SKU de la variante (opcional). */
    @Column(name = "variant_sku")
    private String sku;
}
