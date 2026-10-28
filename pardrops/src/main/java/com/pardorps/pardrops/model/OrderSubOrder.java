package com.pardorps.pardrops.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Parte de un pedido que corresponde a una sola tienda.
 *
 * <p>En un marketplace un mismo pedido puede incluir productos de varios
 * vendedores. Cada vendedor gestiona solo su parte, así que el pedido se
 * divide en subpedidos: uno por tienda. El subpedido tiene su propio estado y
 * sus propios importes, y el estado global del pedido se calcula a partir de
 * ellos.</p>
 *
 * <p>Los productos que todavía no tienen vendedor (el catálogo histórico de la
 * plataforma) forman el subpedido de la tienda propia, con {@code seller} nulo.</p>
 */
@Entity
@Table(name = "order_sub_orders")
@Getter
@Setter
public class OrderSubOrder {

    /** Identificador autogenerado. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Pedido al que pertenece esta parte. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * Vendedor responsable de esta parte.
     *
     * <p>Es nulo cuando los productos pertenecen al catálogo de la plataforma
     * y su gestion sigue correspondiendo al administrador.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private User seller;

    /** Nombre de la tienda en el momento de la compra (copia del historial). */
    @Column(length = 150)
    private String storeName;

    /** Slug de la tienda en el momento de la compra (copia del historial). */
    @Column(length = 180)
    private String storeSlug;

    /** Estado propio de esta parte del pedido. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    /** Suma de las líneas de esta parte antes de impuestos y envío. */
    @Column(nullable = false)
    private Double subtotal;

    /** Gastos de envío imputables a esta parte. */
    @Column(nullable = false)
    private Double shipping;

    /** Importe total de esta parte. */
    @Column(nullable = false)
    private Double total;

    /**
     * Tasa de comisión aplicada a esta venta.
     *
     * <p>Se guarda en el subpedido para que un cambio futuro de la tasa no
     * altere las ventas históricas.</p>
     */
    @Column(name = "commission_rate")
    private Double commissionRate;

    /** Comisión de ParDrops sobre el subtotal de productos. */
    @Column(name = "commission_amount")
    private Double commissionAmount;

    /** Neto que corresponde al vendedor: total menos comisión. */
    @Column(name = "seller_net_amount")
    private Double sellerNetAmount;

    /** Situación de los fondos de la venta. */
    @Enumerated(EnumType.STRING)
    @Column(name = "funds_status", length = 20)
    private FundsStatus fundsStatus;

    /** Fecha en la que el neto pasó a estar disponible. */
    @Column(name = "released_at")
    private Instant releasedAt;

    /** Fecha y hora de creación de la parte. */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Líneas del pedido que pertenecen a esta parte. */
    @OneToMany(mappedBy = "subOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    /**
     * Añade una línea a la parte manteniendo la relación bidireccional.
     *
     * @param item línea a añadir.
     */
    public void addItem(OrderItem item) {
        items.add(item);
        item.setSubOrder(this);
    }
}
