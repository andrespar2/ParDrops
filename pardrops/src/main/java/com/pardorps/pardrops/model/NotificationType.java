package com.pardorps.pardrops.model;

/**
 * Tipos de notificación que genera la plataforma.
 *
 * <p>El tipo permite que el frontend aplique el icono y el estilo adecuados a
 * cada aviso sin tener que interpretar el texto.</p>
 */
public enum NotificationType {

    /** Aviso genérico de la plataforma. */
    INFO,

    /** Resultado de la revisión de una solicitud para ser vendedor. */
    SELLER_APPLICATION,

    /** Aviso relacionado con un pedido o una parte de un pedido. */
    ORDER,

    /** Aviso sobre la moderación de un producto. */
    PRODUCT,

    /** Aviso sobre la cuenta del usuario (suspensión o reactivación). */
    ACCOUNT
}
