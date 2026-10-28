package com.pardorps.pardrops.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Saldo acumulado de un vendedor en la plataforma.
 *
 * <p>El saldo pendiente corresponde a ventas todavía no entregadas; el
 * disponible, a ventas entregadas cuyo neto ya se puede pagar. La comisión
 * acumulada es el 5% descontado por ParDrops.</p>
 */
@Entity
@Table(
        name = "seller_balances",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_seller_balances_seller",
                columnNames = "seller_id")
)
@Getter
@Setter
@NoArgsConstructor
public class SellerBalance {

    /** Identificador autogenerado. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Vendedor propietario del saldo. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    /** Neto retenido de ventas aún no entregadas. */
    @Column(nullable = false)
    private Double pendingAmount = 0.0;

    /** Neto disponible de ventas entregadas. */
    @Column(nullable = false)
    private Double availableAmount = 0.0;

    /** Comisión acumulada de ParDrops. */
    @Column(nullable = false)
    private Double commissionTotal = 0.0;

    /** Subtotal de productos vendidos, sin IVA ni envío. */
    @Column(nullable = false)
    private Double totalSales = 0.0;

    /** Última actualización. */
    @Column(nullable = false)
    private Instant updatedAt;

    /** Inicializa importes y fecha. */
    @PrePersist
    void onCreate() {
        if (pendingAmount == null) pendingAmount = 0.0;
        if (availableAmount == null) availableAmount = 0.0;
        if (commissionTotal == null) commissionTotal = 0.0;
        if (totalSales == null) totalSales = 0.0;
        updatedAt = Instant.now();
    }

    /** Actualiza la fecha en cada cambio. */
    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
