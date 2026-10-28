package com.pardorps.pardrops.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Tienda pública de un vendedor.
 *
 * <p>Se crea automáticamente cuando el administrador aprueba la solicitud
 * para vender en la plataforma. Cada vendedor tiene como mucho una tienda, y
 * sus productos quedan asociados a ella a través de
 * {@link Product#setStore(Store)}.</p>
 */
@Entity
@Table(
        name = "stores",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_stores_slug", columnNames = "slug"),
                @UniqueConstraint(name = "uk_stores_seller", columnNames = "seller_id")
        }
)
@Getter
@Setter
public class Store {

    /** Identificador autogenerado de la tienda. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Vendedor propietario de la tienda. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    /** Nombre comercial visible para los compradores. */
    @Column(nullable = false, length = 120)
    private String name;

    /** Slug único con el que se publica la tienda (/tiendas/{slug}). */
    @Column(nullable = false, length = 140)
    private String slug;

    /** Descripción comercial de la tienda. */
    @Column(length = 1000)
    private String description;

    /** URL del logotipo de la tienda. */
    @Column(length = 500)
    private String logoUrl;

    /** URL de la portada de la tienda. */
    @Column(length = 500)
    private String bannerUrl;

    /** Correo de contacto del vendedor (uso interno y correos transaccionales). */
    @Column(length = 180)
    private String contactEmail;

    /** Teléfono de contacto del vendedor (uso interno). */
    @Column(length = 30)
    private String contactPhone;

    /** Correo público de la tienda, visible para los compradores. */
    @Column(length = 180)
    private String publicEmail;

    /** Teléfono público de la tienda, visible para los compradores. */
    @Column(length = 30)
    private String publicPhone;

    /** Ciudad pública de la tienda. */
    @Column(length = 120)
    private String city;

    /** Correo privado para las notificaciones de pedidos. */
    @Column(length = 180)
    private String notificationEmail;

    /** WhatsApp del vendedor, en formato internacional. */
    @Column(length = 30)
    private String whatsapp;

    /** Estado de la tienda (activa o suspendida). */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StoreStatus status = StoreStatus.ACTIVE;

    /** Fecha de alta de la tienda. */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Fecha de la última modificación. */
    private Instant updatedAt;

    /** Constructor vacío requerido por JPA. */
    public Store() {
    }

    /** Asigna la fecha de alta antes de insertar por primera vez. */
    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = StoreStatus.ACTIVE;
        }
    }

    /** Actualiza la fecha de modificación antes de cada guardado posterior. */
    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
