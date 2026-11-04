package com.pardorps.pardrops.payment;

/**
 * Evento que se publica cuando un pago queda aprobado y registrado.
 *
 * <p>Los correos se envían al confirmar la transacción de base de datos, de
 * modo que un fallo del proveedor de correo nunca revierte una compra.</p>
 *
 * @param orderId pedido confirmado.
 */
public record OrderPaidEvent(Long orderId) {
}
