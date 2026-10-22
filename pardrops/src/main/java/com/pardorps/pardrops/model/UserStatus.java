package com.pardorps.pardrops.model;

/**
 * Estado de la cuenta de un usuario dentro de la plataforma.
 *
 * <p>Una cuenta suspendida conserva sus datos (pedidos, reseñas, tienda) pero
 * no puede iniciar sesión ni realizar acciones. El administrador puede
 * reactivarla en cualquier momento.</p>
 */
public enum UserStatus {

    /** Cuenta activa: puede iniciar sesión y operar con normalidad. */
    ACTIVE,

    /** Cuenta suspendida: el acceso está bloqueado hasta su reactivación. */
    SUSPENDED
}
