package com.pardorps.pardrops.model;

/**
 * Origen de autenticación de una cuenta.
 *
 * <p>Permite distinguir las cuentas creadas con correo y contraseña de las que
 * se crearon o vincularon con Google, sin alterar el rol ni el estado de la
 * cuenta.</p>
 */
public enum AuthProvider {

    /** Cuenta creada con correo y contraseña en ParDrops. */
    LOCAL,

    /** Cuenta creada o vinculada con una identidad de Google. */
    GOOGLE
}
