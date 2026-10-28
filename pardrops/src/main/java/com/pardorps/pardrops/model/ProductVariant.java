package com.pardorps.pardrops.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
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
    @NotBlank(message = "La variante necesita un identificador")
    @Size(max = 80, message = "El identificador de la variante es demasiado largo")
    @Column(name = "variant_id", nullable = false, length = 80)
    private String id;

    /** Nombre descriptivo (por ejemplo, "Talla M", "Color Negro"). */
    @NotBlank(message = "La variante necesita un nombre")
    @Size(max = 120, message = "El nombre de la variante es demasiado largo")
    @Column(name = "variant_name", nullable = false, length = 120)
    private String name;

    /** Precio específico de la variante; si es nulo se usa el del producto. */
    @PositiveOrZero(message = "El precio de la variante no puede ser negativo")
    @Column(name = "variant_price")
    private Double price;

    /** Stock disponible para esta variante; si es nulo se usa el del producto. */
    @PositiveOrZero(message = "El stock de la variante no puede ser negativo")
    @Column(name = "variant_stock")
    private Integer stock;

    /** Código SKU de la variante (opcional). */
    @Size(max = 80, message = "El SKU es demasiado largo")
    @Column(name = "variant_sku", length = 80)
    private String sku;

    /** Talla de la variante (opcional). */
    @Size(max = 30, message = "La talla es demasiado larga")
    @Column(name = "variant_size", length = 30)
    private String size;

    /** Color de la variante (opcional). */
    @Size(max = 40, message = "El color es demasiado largo")
    @Column(name = "variant_color", length = 40)
    private String color;

    /** Imagen de la variante (opcional). */
    @Size(max = 500, message = "La URL de la imagen es demasiado larga")
    @Column(name = "variant_image_url", length = 500)
    private String imageUrl;
}
