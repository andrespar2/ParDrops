package com.pardorps.pardrops.payment;

import java.time.Instant;

/**
 * Datos de una transacción de Wompi relevantes para confirmar un pago.
 *
 * @param id                identificador de la transacción.
 * @param reference         referencia del pedido.
 * @param status            estado (PENDING, APPROVED, DECLINED, VOIDED, ERROR).
 * @param amountInCents     monto en centavos.
 * @param currency          moneda.
 * @param paymentMethodType método de pago (CARD, NEQUI, PSE…).
 * @param customerEmail     correo informado a Wompi.
 * @param statusMessage     mensaje de estado.
 * @param createdAt         fecha de creación en Wompi.
 */
public record WompiTransaction(
        String id,
        String reference,
        String status,
        long amountInCents,
        String currency,
        String paymentMethodType,
        String customerEmail,
        String statusMessage,
        Instant createdAt
) {
}
