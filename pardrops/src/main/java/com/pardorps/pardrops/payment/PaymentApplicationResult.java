package com.pardorps.pardrops.payment;

import com.pardorps.pardrops.model.PaymentStatus;

/**
 * Resultado de aplicar una transacción de Wompi a un pedido.
 *
 * @param paymentStatus    estado resultante del pago.
 * @param applied          indica si esta llamada confirmó la compra.
 * @param alreadyApplied   indica que la compra ya estaba confirmada.
 * @param notificationsNow indica si se generaron las notificaciones internas.
 * @param message          detalle legible del resultado.
 */
public record PaymentApplicationResult(
        PaymentStatus paymentStatus,
        boolean applied,
        boolean alreadyApplied,
        boolean notificationsNow,
        String message
) {
}
