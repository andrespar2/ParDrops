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
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Solicitud de retiro del saldo de un vendedor.
 *
 * <p>Los datos del método de retiro son privados: solo el propio vendedor y el
 * personal administrativo autorizado pueden consultarlos.</p>
 */
@Entity
@Table(name = "withdrawal_requests")
@Getter
@Setter
public class WithdrawalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    /** Valor solicitado (se descuenta del saldo disponible). */
    @Column(nullable = false)
    private Double amount = 0.0;

    /** Comisión de ParDrops. */
    @Column(nullable = false)
    private Double fee = 0.0;

    /** Valor neto que recibe el vendedor. */
    @Column(name = "net_amount", nullable = false)
    private Double netAmount = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WithdrawalMethod method;

    /** Llave Bre-B (privada). */
    @Column(name = "bre_b_key", length = 120)
    private String breBKey;

    @Column(name = "bank_name", length = 120)
    private String bankName;

    @Column(name = "account_type", length = 40)
    private String accountType;

    @Column(name = "account_number", length = 60)
    private String accountNumber;

    @Column(name = "account_holder", length = 120)
    private String accountHolder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WithdrawalStatus status = WithdrawalStatus.PENDING;

    /** Observaciones del vendedor o de administración. */
    @Column(length = 500)
    private String notes;

    /** Referencia de la transferencia al completar. */
    @Column(name = "transfer_reference", length = 120)
    private String transferReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @PrePersist
    void touch() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        updatedAt = createdAt;
    }

    @PreUpdate
    void update() {
        updatedAt = Instant.now();
    }
}
