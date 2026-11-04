package com.pardorps.pardrops.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Registro de un evento recibido de Wompi.
 *
 * <p>La transacción es única: si Wompi reintenta la notificación se actualiza
 * el registro existente en lugar de crear uno nuevo. Guarda solo datos
 * operativos, nunca información sensible de la tarjeta ni los secretos.</p>
 */
@Entity
@Table(
        name = "wompi_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_wompi_event_transaction",
                columnNames = "transaction_id"
        )
)
@Getter
@Setter
public class WompiEvent {

    /** Identificador autogenerado. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador de la transacción en Wompi. */
    @Column(name = "transaction_id", nullable = false, length = 80)
    private String transactionId;

    /** Referencia de pago del pedido. */
    @Column(length = 80)
    private String reference;

    /** Estado notificado por Wompi. */
    @Column(length = 20)
    private String status;

    /** Ambiente del evento ({@code test} o {@code prod}). */
    @Column(length = 10)
    private String environment;

    /** Indica si el checksum del evento se verificó correctamente. */
    @Column(name = "checksum_verified", nullable = false)
    private boolean checksumVerified;

    /** Indica si el pago ya se aplicó al pedido. */
    @Column(nullable = false)
    private boolean processed;

    /** Resumen del motivo de rechazo o del resultado. */
    @Column(name = "result_message", length = 300)
    private String resultMessage;

    /** Fecha de recepción del evento. */
    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    /** Fecha en la que se aplicó el evento. */
    @Column(name = "processed_at")
    private Instant processedAt;

    /** Constructor vacío requerido por JPA. */
    public WompiEvent() {
    }

    /** Asigna la fecha de recepción antes de insertar. */
    @PrePersist
    void onCreate() {
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
    }
}
