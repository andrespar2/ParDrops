package com.pardorps.pardrops.dto;

import com.pardorps.pardrops.model.FundsStatus;
import com.pardorps.pardrops.model.OrderStatus;

import java.time.Instant;
import java.util.List;

/**
 * Representación de una parte del pedido (subpedido de una tienda).
 *
 * <p>Se incluye en el detalle del pedido para que el cliente vea el estado de
 * cada envío por separado y para que la administración conozca la comisión y
 * el neto de cada vendedor.</p>
 *
 * @param id               identificador de la parte.
 * @param storeName        nombre de la tienda que gestiona la parte.
 * @param storeSlug        slug público de la tienda.
 * @param sellerName       nombre del vendedor (nulo si es la tienda propia).
 * @param status           estado de la parte.
 * @param subtotal         suma de las líneas de la parte.
 * @param shipping         gastos de envío de la parte.
 * @param total            importe total de la parte.
 * @param commissionRate   tasa de comisión aplicada.
 * @param commissionAmount comisión de ParDrops.
 * @param sellerNetAmount  neto que corresponde al vendedor.
 * @param fundsStatus      situación de los fondos de la venta.
 * @param items            líneas que componen la parte.
 * @param createdAt        fecha de creación de la parte.
 */
public record OrderSubOrderResponse(
        Long id,
        String storeName,
        String storeSlug,
        String sellerName,
        OrderStatus status,
        Double subtotal,
        Double shipping,
        Double total,
        Double commissionRate,
        Double commissionAmount,
        Double sellerNetAmount,
        FundsStatus fundsStatus,
        List<OrderItemResponse> items,
        Instant createdAt
) {
}
