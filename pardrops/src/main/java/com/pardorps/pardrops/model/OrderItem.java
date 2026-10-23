package com.pardorps.pardrops.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Línea de un pedido: un producto concreto con su cantidad y precio.
 *
 * <p>Se guarda una copia de los datos del producto (nombre, variante y precio)
 * para que el historial no cambie aunque el producto se edite o elimine después
 * desde el panel de administración.</p>
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
public class OrderItem {

    /** Identificador autogenerado. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Pedido al que pertenece la línea. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * Parte del pedido a la que pertenece la línea.
     *
     * <p>Es nula en los pedidos anteriores a la división por vendedores: el
     * historial antiguo se conserva tal cual.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_order_id")
    private OrderSubOrder subOrder;

    /**
     * Vendedor del producto en el momento de la compra.
     *
     * <p>Es nulo cuando el producto pertenece al catálogo de la plataforma.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private User seller;

    /** Referencia al producto original. */
    @Column(nullable = false, length = 36)
    private String productId;

    /** Nombre del producto en el momento de la compra. */
    @Column(nullable = false, length = 200)
    private String productName;

    /** Slug del producto para poder enlazar de nuevo al detalle. */
    @Column(length = 220)
    private String productSlug;

    /** Nombre de la variante elegida (si la hubiera). */
    @Column(length = 120)
    private String variantName;

    /** Precio unitario aplicado. */
    @Column(nullable = false)
    private Double unitPrice;

    /** Cantidad comprada. */
    @Column(nullable = false)
    private int quantity;

    /** Importe total de la línea (precio unitario × cantidad). */
    @Column(nullable = false)
    private Double subtotal;
}
