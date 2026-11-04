package com.pardorps.pardrops.model;

/**
 * Tipo de correo que se envía al confirmar una compra.
 */
public enum PaymentEmailKind {

    /** Confirmación de compra al comprador. */
    BUYER,

    /** Aviso de venta a cada vendedor. */
    SELLER,

    /** Resumen de la venta a los administradores. */
    ADMIN
}
