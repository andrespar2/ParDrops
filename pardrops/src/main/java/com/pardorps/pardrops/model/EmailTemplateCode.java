package com.pardorps.pardrops.model;

/**
 * Tipos de correo transaccional que envía la plataforma.
 *
 * <p>Cada código identifica una plantilla editable y un momento concreto del
 * ciclo del pedido.</p>
 */
public enum EmailTemplateCode {

    /** Orden de compra que recibe el vendedor al crearse el pedido. */
    SELLER_NEW_ORDER,

    /** Comprobante que recibe el comprador cuando el pedido pasa a pagado. */
    BUYER_PAYMENT_RECEIPT,

    /** Aviso que recibe el comprador en cada cambio de estado. */
    BUYER_ORDER_STATUS
}
