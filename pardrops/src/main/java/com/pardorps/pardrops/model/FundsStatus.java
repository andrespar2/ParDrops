package com.pardorps.pardrops.model;

/**
 * Situación de los fondos de una parte del pedido.
 */
public enum FundsStatus {

    /** El dinero está retenido por la plataforma hasta la entrega. */
    HELD,

    /** El neto ya está disponible para el vendedor. */
    RELEASED,

    /** El pedido se canceló y el dinero no se libera. */
    CANCELLED
}
