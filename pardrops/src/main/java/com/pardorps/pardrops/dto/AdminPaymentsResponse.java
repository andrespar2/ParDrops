package com.pardorps.pardrops.dto;

/**
 * Resultado agregado de los pagos para el panel de administración.
 *
 * @param totalTransactions  número de pedidos con referencia de pago.
 * @param approvedCount      pedidos con pago aprobado.
 * @param pendingCount       pedidos con pago pendiente.
 * @param rejectedCount      pedidos con pago rechazado, anulado o con error.
 * @param serviceFeeRevenue  total recaudado por el cargo fijo de servicio.
 * @param commissionRevenue  total de comisiones de ParDrops.
 * @param transactions       detalle de cada transacción.
 */
public record AdminPaymentsResponse(
        long totalTransactions,
        long approvedCount,
        long pendingCount,
        long rejectedCount,
        Double serviceFeeRevenue,
        Double commissionRevenue,
        java.util.List<AdminPaymentTransaction> transactions
) {

    /**
     * Detalle de una transacción de Wompi asociada a un pedido.
     *
     * @param orderId            pedido.
     * @param reference          referencia de pago.
     * @param wompiTransactionId identificador de la transacción.
     * @param paymentStatus      estado del pago.
     * @param orderStatus        estado del pedido.
     * @param paymentMethod      método de pago.
     * @param environment        ambiente.
     * @param customerName       comprador.
     * @param customerEmail      correo del comprador.
     * @param subtotal           suma de productos.
     * @param shipping           envío.
     * @param serviceFee         cargo de servicio.
     * @param total              total cobrado.
     * @param commissionAmount   comisión de ParDrops.
     * @param createdAt          fecha del pedido.
     * @param paidAt             fecha del pago aprobado.
     * @param sellers            distribución por vendedor.
     */
    public record AdminPaymentTransaction(
            Long orderId,
            String reference,
            String wompiTransactionId,
            String paymentStatus,
            String orderStatus,
            String paymentMethod,
            String environment,
            String customerName,
            String customerEmail,
            Double subtotal,
            Double shipping,
            Double serviceFee,
            Double total,
            Double commissionAmount,
            java.time.Instant createdAt,
            java.time.Instant paidAt,
            java.util.List<SellerShare> sellers
    ) {
    }

    /**
     * Parte del importe que corresponde a un vendedor.
     *
     * @param storeName  tienda.
     * @param sellerName vendedor.
     * @param subtotal   importe de sus productos.
     * @param commission comisión de ParDrops.
     * @param net        importe acreditado.
     * @param fundsStatus situación de los fondos.
     */
    public record SellerShare(
            String storeName,
            String sellerName,
            Double subtotal,
            Double commission,
            Double net,
            String fundsStatus
    ) {
    }
}
