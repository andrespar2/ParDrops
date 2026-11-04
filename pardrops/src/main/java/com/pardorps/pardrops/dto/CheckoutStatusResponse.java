package com.pardorps.pardrops.dto;

import com.pardorps.pardrops.model.PaymentStatus;

import java.time.Instant;

/**
 * Estado de un checkout consultado por la pantalla de resultado.
 *
 * <p>{@code verifiedPayment} solo es verdadero cuando el backend comprobó la
 * transacción con Wompi. La URL de retorno nunca se usa como prueba.</p>
 *
 * @param reference       referencia del pago.
 * @param order           pedido con su desglose económico.
 * @param paymentStatus   estado del pago.
 * @param verifiedPayment indica si el pago fue verificado con Wompi.
 * @param canRetry        indica si se puede reintentar el pago.
 * @param message         mensaje listo para mostrar al comprador.
 * @param checkedAt       momento de la consulta.
 */
public record CheckoutStatusResponse(
        String reference,
        OrderResponse order,
        PaymentStatus paymentStatus,
        boolean verifiedPayment,
        boolean canRetry,
        String message,
        Instant checkedAt
) {
}
