package com.pardorps.pardrops.model;

/**
 * Estados de una tienda de vendedor.
 *
 * <p>La tienda se crea automáticamente al aprobar la solicitud de vendedor y
 * permanece activa mientras el vendedor existe. El administrador puede
 * suspenderla para retirar temporalmente sus productos del marketplace.</p>
 */
public enum StoreStatus {

    /** Tienda activa: visible en el catálogo y con productos publicados. */
    ACTIVE,

    /** Tienda suspendida: sus productos quedan ocultos del catálogo. */
    SUSPENDED,

    /**
     * Tienda eliminada por el administrador.
     *
     * <p>Es una eliminación lógica: la tienda y su historial se conservan en la
     * base de datos, pero deja de mostrarse en el marketplace y en los listados
     * operativos. Puede restaurarse desde el panel.</p>
     */
    DELETED
}
