package com.pardorps.pardrops.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Ingreso propio de ParDrops asociado a un pedido.
 *
 * <p>Se usa para registrar el cargo fijo de servicio de 700 COP. La
 * restricción única por pedido y concepto garantiza que un evento repetido o
 * un reintento no duplique el ingreso.</p>
 */
@Entity
@Table(
        name = "platform_revenues",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_platform_revenue_order_type",
                columnNames = {"order_id", "type"}
        )
)
@Getter
@Setter
public class PlatformRevenue {

    /** Identificador autogenerado. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Pedido que originó el ingreso. */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** Referencia de pago del pedido. */
    @Column(name = "payment_reference", length = 80)
    private String paymentReference;

    /** Concepto del ingreso. */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private PlatformRevenueType type;

    /** Importe del ingreso. */
    @Column(nullable = false)
    private Double amount;

    /** Moneda del importe. */
    @Column(length = 3)
    private String currency = "COP";

    /** Identificador de la transacción de Wompi. */
    @Column(name = "wompi_transaction_id", length = 80)
    private String wompiTransactionId;

    /** Descripción legible. */
    @Column(length = 250)
    private String description;

    /** Fecha y hora del registro. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Constructor vacío requerido por JPA. */
    public PlatformRevenue() {
    }

    /** Asigna la fecha antes de insertar. */
    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
