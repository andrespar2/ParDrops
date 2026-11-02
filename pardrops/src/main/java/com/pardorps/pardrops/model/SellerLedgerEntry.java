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
 * Movimiento económico del vendedor (ledger).
 *
 * <p>Cada venta liberada y cada retiro dejan un registro inmutable con el saldo
 * resultante, para poder reconstruir el dinero del vendedor.</p>
 */
@Entity
@Table(name = "seller_ledger")
@Getter
@Setter
public class SellerLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LedgerEntryType type;

    /** Referencia de la venta o solicitud. */
    @Column(length = 120)
    private String reference;

    @Column(length = 200)
    private String description;

    @Column(nullable = false)
    private Double amount = 0.0;

    @Column(nullable = false)
    private Double commission = 0.0;

    @Column(name = "balance_after", nullable = false)
    private Double balanceAfter = 0.0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void touch() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
