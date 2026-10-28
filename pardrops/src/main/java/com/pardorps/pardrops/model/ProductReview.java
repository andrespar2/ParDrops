package com.pardorps.pardrops.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Reseña pública que un cliente deja sobre un producto que ha comprado.
 *
 * <p>Cada usuario puede publicar como máximo una reseña por producto, lo que
 * se garantiza con una restricción única sobre la pareja
 * {@code (product_id, user_id)}. El autor se guarda como relación con
 * {@link User} para poder mostrar su nombre, pero nunca su correo.</p>
 *
 * <p>La calificación va de 1 a 5 estrellas y el comentario recoge la
 * experiencia del cliente con el producto.</p>
 */
@Entity
@Table(
        name = "product_reviews",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_product_reviews_product_user",
                columnNames = {"product_id", "user_id"}
        )
)
@Getter
@Setter
public class ProductReview {

    /** Identificador autogenerado de la reseña. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Producto valorado. Sus reseñas se eliminan junto con él. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Usuario que escribe la reseña (solo clientes con compra entregada). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Calificación otorgada, de 1 a 5 estrellas. */
    @Column(nullable = false)
    private int rating;

    /** Comentario escrito por el cliente sobre su experiencia. */
    @Column(nullable = false, length = 2000)
    private String comment;

    /** URLs de las imágenes adjuntas a la reseña. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "product_review_images",
            joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "image_url", length = 1000)
    private java.util.List<String> images = new java.util.ArrayList<>();

    /** Fecha y hora de publicación de la reseña. */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Constructor vacío requerido por JPA. */
    public ProductReview() {
    }

    /** Fija la fecha de publicación antes de insertar por primera vez. */
    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
