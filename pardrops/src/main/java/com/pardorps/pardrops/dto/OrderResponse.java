package com.pardorps.pardrops.dto;

import com.pardorps.pardrops.model.OrderStatus;

import java.time.Instant;
import java.util.List;

/**
 * Representación de un pedido devuelta por la API.
 *
 * <p>Evita exponer directamente las entidades y sus relaciones perezosas, y
 * permite incluir, solo para el administrador, el correo del usuario asociado.</p>
 *
 * @param id                identificador del pedido.
 * @param customerName      nombre de quien recibe el pedido.
 * @param customerEmail     correo de contacto.
 * @param customerPhone     teléfono de contacto.
 * @param addressStreet     calle y número.
 * @param addressCity       ciudad.
 * @param addressPostalCode código postal.
 * @param addressCountry    país.
 * @param notes             comentarios del cliente.
 * @param items             líneas del pedido.
 * @param subOrders         partes del pedido, una por tienda.
 * @param subtotal          suma de las líneas.
 * @param tax               impuestos aplicados.
 * @param shipping          gastos de envío.
 * @param total             importe total.
 * @param status            estado del pedido.
 * @param createdAt         fecha de creación.
 * @param userEmail         correo de la cuenta asociada (nulo si es invitado).
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
        Double tax,
        Double shipping,
        Double total,
        OrderStatus status,
        Instant createdAt,
        String userEmail
) {
}
