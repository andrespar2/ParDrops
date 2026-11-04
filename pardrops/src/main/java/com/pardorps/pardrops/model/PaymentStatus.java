package com.pardorps.pardrops.model;

/**
 * Estado del pago de un pedido en Wompi.
 *
 * <p>Solo {@link #APPROVED} confirma una compra. Los estados
 * {@link #DECLINED}, {@link #VOIDED} y {@link #ERROR} son finales y no
 * acreditan saldos a los vendedores.</p>
 */
public enum PaymentStatus {

    /** El checkout se creó, pero Wompi todavía no ha confirmado el pago. */
    PENDING,

    /** Wompi confirmó el pago y la transacción quedó aprobada. */
    APPROVED,

    /** Wompi rechazó el pago. */
    DECLINED,

    /** La transacción fue anulada. */
    VOIDED,

    /** Wompi informó de un error al procesar la transacción. */
    ERROR;

    /**
     * Indica si el pago ya no puede cambiar de estado.
     *
     * @return {@code true} para los estados finales.
     */
    public boolean isFinal() {
        return this == APPROVED || this == DECLINED || this == VOIDED || this == ERROR;
    }

    /**
     * Indica si el pago fue aprobado.
     *
     * @return {@code true} únicamente para {@link #APPROVED}.
     */
    public boolean isApproved() {
        return this == APPROVED;
    }
}
