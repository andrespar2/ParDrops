package com.pardorps.pardrops.model;

/**
 * Roles disponibles para los usuarios del sistema.
 *
 * <p>Se usan para autorizar el acceso a las distintas partes de la API:
 * los usuarios con {@link #ADMIN} gestionan el catálogo y todo el marketplace,
 * los {@link #SELLER} gestionan su propia tienda y sus pedidos, mientras que
 * los {@link #CUSTOMER} solo gestionan su propia información.</p>
 */
public enum Role {

    /** Cliente estándar: compra productos y consulta su propio historial. */
    CUSTOMER,

    /** Vendedor: gestiona su tienda, su catálogo y la parte de sus pedidos. */
    SELLER,

    /** Administrador: gestiona el catálogo de productos y todo el marketplace. */
    ADMIN
}
