package com.pardorps.pardrops.model;

/**
 * Nivel de un administrador dentro de ParDrops.
 *
 * <p>{@link #OWNER} es el administrador principal: solo él puede crear o
 * modificar cuentas administrativas. {@link #MANAGER} gestiona la operación
 * diaria y {@link #SUPPORT} atiende el soporte a usuarios.</p>
 */
public enum AdminLevel {

    /** Administrador principal, con control total. */
    OWNER,

    /** Administrador de operación: catálogo, pedidos y vendedores. */
    MANAGER,

    /** Administrador de soporte: conversaciones y usuarios. */
    SUPPORT
}
