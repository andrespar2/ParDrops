package com.pardorps.pardrops.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Imagen asociada a un producto.
 *
 * <p>Se modela como un tipo embebido ({@code @Embeddable}) porque una imagen
 * no tiene sentido ni identidad por sí misma fuera de su producto: forma parte
 * de la colección de imágenes de {@link Product}.</p>
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductImage {

    /** URL pública de la imagen. */
    @Column(name = "url", nullable = false, length = 1000)
    private String url;

    /** Texto alternativo para accesibilidad. */
    @Column(name = "alt", length = 255)
    private String alt;

    /**
     * Indica si es la imagen principal que se muestra por defecto.
     *
     * <p>Se exponen accesores manuales para que el JSON conserve el nombre
     * {@code isPrimary} (el contrato que consume el frontend) en lugar del
     * nombre {@code primary} que generaría Lombok a partir del prefijo "is".</p>
     */
    @Column(name = "is_primary")
    private boolean primary;

    /** @return {@code true} si la imagen es la principal. */
    @JsonProperty("isPrimary")
    public boolean isPrimary() {
        return primary;
    }

    /** @param primary nuevo valor del indicador de imagen principal. */
    @JsonProperty("isPrimary")
    public void setPrimary(boolean primary) {
        this.primary = primary;
    }
}
