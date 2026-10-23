package com.pardorps.pardrops.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Categoría del catálogo del marketplace.
 *
 * <p>Hasta la evolución a marketplace las categorías eran cadenas de texto
 * libres dentro de {@link Product} ({@code @ElementCollection}). Esta entidad
 * las convierte en un catálogo gestionable desde el panel de administración
 * (nombre, slug, descripción y estado), sin romper los valores ya guardados:
 * la migración inicial las importa automáticamente.</p>
 *
 * <p>Los productos siguen guardando el <em>nombre</em> de la categoría, de modo
 * que el catálogo histórico permanece intacto aunque una categoría se
 * desactive.</p>
 */
@Entity
@Table(
        name = "categories",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_categories_slug", columnNames = "slug"),
                @UniqueConstraint(name = "uk_categories_name", columnNames = "name")
        }
)
@Getter
@Setter
public class Category {

    /** Identificador autogenerado de la categoría. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre visible de la categoría (coincide con el valor de producto). */
    @Column(nullable = false, length = 120)
    private String name;

    /** Slug único con el que se publica la categoría. */
    @Column(nullable = false, length = 140)
    private String slug;

    /** Descripción de la categoría (opcional). */
    @Column(length = 500)
    private String description;

    /** Indica si la categoría está disponible para asignarla a productos. */
    @Column(nullable = false)
    private boolean active = true;

    /** Indica si la categoría se muestra en el menú de navegación del catálogo. */
    @Column(nullable = false)
    private boolean featured;

    /** Fecha de alta de la categoría. */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Fecha de la última modificación. */
    private Instant updatedAt;

    /** Constructor vacío requerido por JPA. */
    public Category() {
    }

    /** Asigna fechas y valores por defecto antes de insertar por primera vez. */
    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    /** Actualiza la fecha de modificación antes de cada guardado posterior. */
    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
