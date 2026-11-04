package com.pardorps.pardrops.dto;

/**
 * Datos que el frontend necesita para abrir el widget de Wompi.
 *
 * <p>Incluye únicamente la llave pública y la firma de integridad; las llaves
 * privadas y los secretos permanecen en el backend.</p>
 *
 * @param orderId            identificador del pedido creado.
 * @param reference          referencia única de pago.
 * @param amountInCents      monto que se cobra en Wompi (centavos).
 * @param currency           moneda del cobro.
 * @param publicKey          llave pública del ambiente activo.
 * @param integritySignature firma SHA-256 calculada en el servidor.
 * @param redirectUrl        URL a la que Wompi devuelve al comprador.
 * @param environment        ambiente activo ({@code test} o {@code prod}).
 * @param customerName       nombre del comprador.
 * @param customerEmail      correo del comprador.
 * @param customerPhone      teléfono del comprador.
 * @param subtotal           suma de los productos.
 * @param shipping           gastos de envío.
 * @param serviceFee         cargo fijo de servicio de ParDrops.
 * @param total              total que se cobra.
 */
public record CheckoutResponse(
        Long orderId,
        String reference,
        long amountInCents,
        String currency,
        String publicKey,
        String integritySignature,
        String redirectUrl,
        String environment,
        String customerName,
        String customerEmail,
        String customerPhone,
        Double subtotal,
        Double shipping,
        Double serviceFee,
        Double total,
        java.util.List<OrderItemResponse> items
) {
}
