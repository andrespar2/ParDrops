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
 * CategorÃ­a del catÃ¡logo del marketplace.
 *
 * <p>Hasta la evoluciÃ³n a marketplace las categorÃ­as eran cadenas de texto
 * libres dentro de {@link Product} ({@code @ElementCollection}). Esta entidad
 * las convierte en un catÃ¡logo gestionable desde el panel de administraciÃ³n
 * (nombre, slug, descripciÃ³n y estado), sin romper los valores ya guardados:
 * la migraciÃ³n inicial las importa automÃ¡ticamente.</p>
 *
 * <p>Los productos siguen guardando el <em>nombre</em> de la categorÃ­a, de modo
 * que el catÃ¡logo histÃ³rico permanece intacto aunque una categorÃ­a se
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

    /** Identificador autogenerado de la categorÃ­a. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre visible de la categorÃ­a (coincide con el valor de producto). */
    @Column(nullable = false, length = 120)
    private String name;

    /** Slug Ãºnico con el que se publica la categorÃ­a. */
    @Column(nullable = false, length = 140)
    private String slug;

    /** DescripciÃ³n de la categorÃ­a (opcional). */
    @Column(length = 500)
    private String description;

    /** Indica si la categorÃ­a estÃ¡ disponible para asignarla a productos. */
    @Column(nullable = false)
    private boolean active = true;

    /** Indica si la categorÃ­a se muestra en el menÃº de navegaciÃ³n del catÃ¡logo. */
    @Column(nullable = false)
    private boolean featured;

    /** CategorÃ­a padre, para organizar subcategorÃ­as. */
    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "parent_id")
    private Category parent;

    /** Orden de apariciÃ³n en el catÃ¡logo. */
    @Column(name = "sort_order")
    private int sortOrder = 0;

    /** Imagen representativa de la categorÃ­a. */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** Indica si es una colecciÃ³n (ofertas, novedades, temporadas). */
    @Column
    private boolean collection = false;

    /** Fecha de alta de la categorÃ­a. */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Fecha de la Ãºltima modificaciÃ³n. */
    private Instant updatedAt;

    /** Constructor vacÃ­o requerido por JPA. */
    public Category() {
    }

    /** Asigna fechas y valores por defecto antes de insertar por primera vez. */
    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    /** Actualiza la fecha de modificaciÃ³n antes de cada guardado posterior. */
    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}

