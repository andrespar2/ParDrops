package com.pardorps.pardrops.model;

/**
 * Estados posibles por los que pasa un pedido.
 */
public enum OrderStatus {

    /** Pedido registrado, pendiente de pago o preparación. */
    PENDING,

    /** Pedido pagado. */
    PAID,

    /** Pedido enviado al cliente. */
    SHIPPED,

    /** Pedido entregado. */
    DELIVERED,

    /** Pedido cancelado. */
    CANCELLED
}
