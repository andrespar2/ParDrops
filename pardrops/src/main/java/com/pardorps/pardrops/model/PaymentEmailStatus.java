package com.pardorps.pardrops.model;

/**
 * Resultado del envío de un correo de confirmación de compra.
 */
public enum PaymentEmailStatus {

    /** El envío se intentó pero falló; admite reintento. */
    FAILED,

    /** El correo se entregó al proveedor. */
    SENT
}
