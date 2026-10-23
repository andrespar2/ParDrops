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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Solicitud de un cliente para convertirse en vendedor en la plataforma.
 *
 * <p>El usuario la envía con los datos de su negocio y queda a la espera de
 * que el administrador la apruebe o la rechace. Al aprobarla se crea
 * automáticamente su {@link Store} y su rol pasa a ser
 * {@link Role#SELLER}.</p>
 */
@Entity
@Table(name = "seller_applications")
@Getter
@Setter
public class SellerApplication {

    /** Identificador autogenerado de la solicitud. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Usuario que solicita convertirse en vendedor. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Nombre comercial del negocio. */
    @Column(nullable = false, length = 120)
    private String businessName;

    /** NIT o identificación tributaria del negocio. */
    @Column(length = 40)
    private String taxId;

    /** Descripción de los productos que se venderán. */
    @Column(length = 2000)
    private String description;

    /** Teléfono de contacto del negocio. */
    @Column(length = 30)
    private String phone;

    /** Ciudad desde la que se envía el producto. */
    @Column(length = 120)
    private String city;

    /** Estado de la solicitud. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SellerApplicationStatus status = SellerApplicationStatus.PENDING;

    /** Comentario del administrador al aprobar o rechazar. */
    @Column(length = 500)
    private String reviewNotes;

    /** Administrador que revisó la solicitud (nulo mientras esté pendiente). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    /** Fecha en que se revisó la solicitud. */
    private Instant reviewedAt;

    /** Fecha de envío de la solicitud. */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Constructor vacío requerido por JPA. */
    public SellerApplication() {
    }

    /** Asigna la fecha de envío antes de insertar por primera vez. */
    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = SellerApplicationStatus.PENDING;
        }
    }
}
