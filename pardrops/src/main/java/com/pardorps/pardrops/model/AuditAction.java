package com.pardorps.pardrops.model;

/**
 * Acciones relevantes que quedan registradas en la bitácora de auditoría.
 *
 * <p>Se registran las decisiones del administrador que affectan a terceros
 * (aprobaciones de vendedores, suspensiones de cuentas, moderación del
 * catálogo) para poder reconstruir quién hizo qué y cuándo.</p>
 */
public enum AuditAction {

    /** Solicitud de vendedor aprobada. */
    SELLER_APPLICATION_APPROVED,

    /** Solicitud de vendedor rechazada. */
    SELLER_APPLICATION_REJECTED,

    /** Cuenta de usuario suspendida. */
    USER_SUSPENDED,

    /** Cuenta de usuario reactivada. */
    USER_REACTIVATED,

    /** Producto moderado por el administrador. */
    PRODUCT_MODERATED,

    /** Producto eliminado por el administrador. */
    PRODUCT_DELETED,

    /** Categoría creada. */
    CATEGORY_CREATED,

    /** Categoría modificada. */
    CATEGORY_UPDATED,

    /** Categoría eliminada. */
    CATEGORY_DELETED,

    /** Tienda suspendida por el administrador. */
    STORE_SUSPENDED,

    /** Tienda reactivada por el administrador. */
    STORE_REACTIVATED,

    /** Cuenta creada con Google. */
    GOOGLE_ACCOUNT_CREATED,

    /** Cuenta local vinculada con Google. */
    GOOGLE_ACCOUNT_LINKED,

    /** Plantilla de correo actualizada por el administrador. */
    EMAIL_TEMPLATE_UPDATED,

    /** Plantilla de correo restaurada a sus valores predeterminados. */
    EMAIL_TEMPLATE_RESET,

    /** Reseña eliminada por el administrador. */
    REVIEW_DELETED
}
