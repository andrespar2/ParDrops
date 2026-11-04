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
 * Registro del envío de un correo de confirmación de compra.
 *
 * <p>La combinación pedido + tipo + destinatario es única, de modo que un
 * evento repetido de Wompi o una recarga de la pantalla de resultado no
 * vuelven a enviar el mismo correo. Si el primer intento falla, el registro
 * queda en {@link PaymentEmailStatus#FAILED} y se permite reintentar.</p>
 */
@Entity
@Table(
        name = "payment_email_logs",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payment_email_order_kind_recipient",
                columnNames = {"order_id", "kind", "recipient"}
        )
)
@Getter
@Setter
public class PaymentEmailLog {

    /** Identificador autogenerado. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Pedido al que corresponde el correo. */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** Tipo de correo. */
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private PaymentEmailKind kind;

    /** Correo del destinatario. */
    @Column(name = "recipient", nullable = false, length = 180)
    private String recipient;

    /** Resultado del último intento. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentEmailStatus status;

    /** Número de intentos realizados. */
    @Column(nullable = false)
    private int attempts;

    /** Último error registrado (sin datos sensibles). */
    @Column(name = "last_error", length = 300)
    private String lastError;

    /** Fecha de creación del registro. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Fecha del último envío correcto. */
    @Column(name = "sent_at")
    private Instant sentAt;

    /** Constructor vacío requerido por JPA. */
    public PaymentEmailLog() {
    }

    /** Asigna la fecha de creación antes de insertar. */
    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
