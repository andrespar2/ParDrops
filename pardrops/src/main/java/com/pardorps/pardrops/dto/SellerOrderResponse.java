package com.pardorps.pardrops.dto;

import com.pardorps.pardrops.model.OrderStatus;

import java.time.Instant;
import java.util.List;

/**
 * Representación de la parte de un pedido que corresponde a un vendedor.
 *
 * <p>Incluye los datos de envío del cliente porque el vendedor es quien
 * prepara y entrega su parte. Solo se devuelve para las partes del propio
 * vendedor: ningún otro ve estos datos.</p>
 *
 * @param id                identificador de la parte.
 * @param orderId           pedido al que pertenece.
 * @param storeName         nombre de la tienda del vendedor.
 * @param customerName      nombre de quien recibe el pedido.
 * @param customerPhone     teléfono de contacto del cliente.
 * @param addressStreet     calle y número de la dirección de envío.
 * @param addressCity       ciudad de la dirección de envío.
 * @param addressPostalCode código postal de la dirección de envío.
 * @param addressCountry    país de la dirección de envío.
 * @param status            estado de la parte.
 * @param subtotal          suma de las líneas de la parte.
 * @param shipping          gastos de envío de la parte.
 * @param total             importe total de la parte.
 * @param items             líneas que componen la parte.
 * @param createdAt         fecha de creación de la parte.
 */
public record SellerOrderResponse(
        Long id,
        Long orderId,
        String storeName,
        String customerName,
        String customerPhone,
        String addressStreet,
        String addressCity,
        String addressPostalCode,
        String addressCountry,
        OrderStatus status,
        Double subtotal,
        Double shipping,
        Double total,
        List<OrderItemResponse> items,
        Instant createdAt
) {
}
