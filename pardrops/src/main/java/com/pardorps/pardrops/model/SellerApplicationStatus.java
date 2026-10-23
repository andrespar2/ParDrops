package com.pardorps.pardrops.model;

/**
 * Estados por los que pasa una solicitud para convertirse en vendedor.
 *
 * <p>El flujo normal es {@link #PENDING} → {@link #APPROVED} o
 * {@link #REJECTED}. Una solicitud rechazada puede volver a presentarse como una
 * solicitud nueva; una aprobada es definitiva.</p>
 */
public enum SellerApplicationStatus {

    /** Solicitud enviada y a la espera de que el administrador la revise. */
    PENDING,

    /** Solicitud aprobada: el usuario pasa a ser vendedor con su tienda activa. */
    APPROVED,

    /** Solicitud rechazada: el administrador dejó constancia del motivo. */
    REJECTED
}
