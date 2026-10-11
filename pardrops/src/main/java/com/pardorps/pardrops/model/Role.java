package com.pardorps.pardrops.model;

/**
 * Roles disponibles para los usuarios del sistema.
 *
 * <p>Se usan para autorizar el acceso a las distintas partes de la API:
 * los usuarios con {@link #ADMIN} pueden gestionar productos y ver todos
 * los pedidos, mientras que los {@link #CUSTOMER} solo gestionan su propia
 * información.</p>
 */
public enum Role {

    /** Cliente estándar: compra productos y consulta su propio historial. */
    CUSTOMER,

    /** Administrador: gestiona el catálogo de productos de la tienda. */
    ADMIN
}
