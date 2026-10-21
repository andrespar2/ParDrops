package com.pardorps.pardrops.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidad que representa un producto del catálogo.
 *
 * <p>El identificador es un {@code UUID} en formato texto para que coincida con
 * el tipo {@code string} que maneja el frontend. Las imágenes, categorías,
 * etiquetas y variantes se guardan en tablas secundarias mediante
 * {@code @ElementCollection}, ya que son valores que pertenecen al producto.</p>
 */
@Entity
@Table(
        name = "products",
        uniqueConstraints = @UniqueConstraint(name = "uk_products_slug", columnNames = "slug")
)
@Getter
@Setter
public class Product {

    /** Identificador público del producto (UUID en texto). */
    @Id
    @Column(length = 36)
    private String id;

    /** Nombre visible del producto. */
    @Column(nullable = false, length = 200)
    private String name;

    /** Slug único para construir URLs amigables (/productos/{slug}). */
    @Column(nullable = false, length = 220)
    private String slug;

    /** Descripción corta que se muestra en los listados. */
    @Column(nullable = false, length = 500)
    private String description;

    /** Descripción larga que se muestra en el detalle (opcional). */
    @Column(length = 4000)
    private String longDescription;

    /** Precio actual del producto. */
    @Column(nullable = false)
    private Double price;

    /** Precio antes de la rebaja; si es nulo no hay descuento. */
    private Double originalPrice;

    /** Código de moneda ISO 4217 (por defecto COP). */
    @Column(nullable = false, length = 3)
    private String currency = "COP";

    /** Imágenes del producto. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    private List<ProductImage> images = new ArrayList<>();

    /** Categorías a las que pertenece el producto. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_categories", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "category", nullable = false, length = 120)
    private List<String> categories = new ArrayList<>();

    /** Etiquetas usadas para búsquedas y filtros. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_tags", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "tag", nullable = false, length = 120)
    private List<String> tags = new ArrayList<>();

    /** Stock total disponible del producto. */
    @Column(nullable = false)
    private int stock;

    /** Variantes disponibles del producto. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_variants", joinColumns = @JoinColumn(name = "product_id"))
    private List<ProductVariant> variants = new ArrayList<>();

    /** Valoración media (0 a 5). */
    private Double rating;

    /** Número de reseñas recibidas. */
    private Integer reviewCount;

    /** Indica si el producto es una novedad. */
    @Column(nullable = false)
    private boolean isNew;

    /** Indica si el producto está destacado en la portada. */
    @Column(nullable = false)
    private boolean isFeatured;

    /** Indica si el producto está en oferta. */
    @Column(nullable = false)
    private boolean onSale;

    /** Fecha de alta del producto. */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Fecha de la última modificación. */
    private Instant updatedAt;

    /** Constructor vacío requerido por JPA. */
    public Product() {
    }

    /** Asigna identificador y fechas antes de insertar por primera vez. */
    @PrePersist
    void onCreate() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    /** Actualiza la fecha de modificación antes de cada guardado posterior. */
    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    // ------------------------------------------------------------------
    // Accesores de los indicadores booleanos.
    //
    // Lombok nombraría los métodos como isNew()/isFeatured()/onSale y Jackson
    // los publicaría como "new"/"featured"/"onSale", rompiendo el contrato que
    // espera el frontend ("isNew"/"isFeatured"/"onSale"). Por eso se escriben
    // a mano y se fija explícitamente el nombre de la propiedad JSON.
    // ------------------------------------------------------------------

    /** @return {@code true} si es una novedad. */
    @JsonProperty("isNew")
    public boolean isNew() {
        return isNew;
    }

    /** @param isNew nuevo valor del indicador de novedad. */
    @JsonProperty("isNew")
    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    /** @return {@code true} si está destacado. */
    @JsonProperty("isFeatured")
    public boolean isFeatured() {
        return isFeatured;
    }

    /** @param isFeatured nuevo valor del indicador de destacado. */
    @JsonProperty("isFeatured")
    public void setFeatured(boolean isFeatured) {
        this.isFeatured = isFeatured;
    }

    /** @return {@code true} si está en oferta. */
    @JsonProperty("onSale")
    public boolean isOnSale() {
        return onSale;
    }

    /** @param onSale nuevo valor del indicador de oferta. */
    @JsonProperty("onSale")
    public void setOnSale(boolean onSale) {
        this.onSale = onSale;
    }
}
