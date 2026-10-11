package com.pardorps.pardrops.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pardorps.pardrops.model.ProductImage;
import com.pardorps.pardrops.model.ProductVariant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Datos que el administrador envía para crear o editar un producto.
 *
 * <p>No incluye {@code id}, {@code createdAt} ni {@code updatedAt}: el primero
 * lo genera el servidor y las fechas se gestionan automáticamente. Los campos
 * booleanos se reciben como {@link Boolean} para poder distinguir "no enviado"
 * de "falso".</p>
 *
 * @param name            nombre del producto.
 * @param slug            slug único para la URL.
 * @param description     descripción corta.
 * @param longDescription descripción detallada (opcional).
 * @param price           precio actual.
 * @param originalPrice   precio anterior, si está en oferta (opcional).
 * @param currency        código de moneda (por defecto EUR).
 * @param images          imágenes del producto.
 * @param categories      categorías del producto.
 * @param tags            etiquetas de búsqueda.
 * @param stock           unidades disponibles.
 * @param variants        variantes del producto.
 * @param rating          valoración media (opcional).
 * @param reviewCount     número de reseñas (opcional).
 * @param isNew           marca de novedad.
 * @param isFeatured      marca de destacado.
 * @param onSale          marca de oferta.
 */
public record ProductRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 200, message = "El nombre es demasiado largo")
        String name,

        @NotBlank(message = "El slug es obligatorio")
        @Size(max = 220, message = "El slug es demasiado largo")
        String slug,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 500, message = "La descripción es demasiado larga")
        String description,

        @Size(max = 4000, message = "La descripción larga es demasiado extensa")
        String longDescription,

        @NotNull(message = "El precio es obligatorio")
        @PositiveOrZero(message = "El precio no puede ser negativo")
        Double price,

        @PositiveOrZero(message = "El precio original no puede ser negativo")
        Double originalPrice,

        @Size(max = 3, message = "El código de moneda no es válido")
        String currency,

        @Valid
        List<ProductImage> images,

        List<String> categories,

        List<String> tags,

        @NotNull(message = "El stock es obligatorio")
        @PositiveOrZero(message = "El stock no puede ser negativo")
        Integer stock,

        @Valid
        List<ProductVariant> variants,

        Double rating,

        Integer reviewCount,

        @JsonProperty("isNew")
        Boolean isNew,

        @JsonProperty("isFeatured")
        Boolean isFeatured,

        @JsonProperty("onSale")
        Boolean onSale
) {
}
