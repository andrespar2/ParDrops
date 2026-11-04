package com.pardorps.pardrops.dto;

import com.pardorps.pardrops.model.OrderStatus;
import com.pardorps.pardrops.model.PaymentStatus;

import java.time.Instant;
import java.util.List;

/**
 * Representación de un pedido devuelta por la API.
 *
 * <p>Evita exponer directamente las entidades y sus relaciones perezosas, y
 * permite incluir, solo para el administrador, el correo del usuario asociado.</p>
 *
 * @param id                 identificador del pedido.
 * @param customerName       nombre de quien recibe el pedido.
 * @param customerEmail      correo de contacto.
 * @param customerPhone      teléfono de contacto.
 * @param addressStreet      calle y número.
 * @param addressCity        ciudad.
 * @param addressPostalCode  código postal.
 * @param addressCountry     país.
 * @param notes              comentarios del cliente.
 * @param items              líneas del pedido.
 * @param subOrders          partes del pedido, una por tienda.
 * @param subtotal           suma de las líneas.
 * @param shipping           gastos de envío.
 * @param total              importe total.
 * @param status             estado del pedido.
 * @param createdAt          fecha de creación.
 * @param paymentReference   referencia única de pago (nula si no usa Wompi).
 * @param paymentStatus      estado del pago en Wompi.
 * @param paymentMethod      método de pago informado por Wompi.
 * @param wompiTransactionId identificador de la transacción.
 * @param serviceFee         cargo fijo de servicio de ParDrops.
 * @param paidAt             fecha en la que se aprobó el pago.
 * @param paymentEnvironment ambiente de Wompi ({@code test} o {@code prod}).
 * @param userEmail          correo de la cuenta asociada (nulo si es invitado).
 */
public record OrderResponse(
        Long id,
        String customerName,
        String customerEmail,
        String customerPhone,
        String addressStreet,
        String addressCity,
        String addressPostalCode,
        String addressCountry,
        String notes,
        List<OrderItemResponse> items,
        List<OrderSubOrderResponse> subOrders,
        Double subtotal,
        Double shipping,
        Double total,
        OrderStatus status,
        Instant createdAt,
        String paymentReference,
        PaymentStatus paymentStatus,
        String paymentMethod,
        String wompiTransactionId,
        Double serviceFee,
        Instant paidAt,
        String paymentEnvironment,
        String userEmail
) {
}
