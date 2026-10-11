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
 * Pedido realizado en la tienda.
 *
 * <p>Guarda los datos del cliente que se piden en el checkout (nombre, correo,
 * teléfono y dirección) y el desglose económico (subtotal, impuestos, envío y
 * total). Cuando el comprador ha iniciado sesión, se enlaza con su {@link User}
 * para poder consultar su historial; en cambio, un pedido de invitado se guarda
 * con {@code user} nulo para no asociarse a ninguna cuenta.</p>
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {

    /** Identificador autogenerado del pedido. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Usuario que realizó el pedido.
     * Es nulo para los pedidos hechos como invitado (sin cuenta).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** Nombre de la persona que recibe el pedido. */
    @Column(nullable = false, length = 120)
    private String customerName;

    /** Correo de contacto del pedido. */
    @Column(nullable = false, length = 180)
    private String customerEmail;

    /** Teléfono de contacto del pedido. */
    @Column(nullable = false, length = 30)
    private String customerPhone;

    /** Calle y número de la dirección de envío. */
    @Column(nullable = false, length = 250)
    private String addressStreet;

    /** Ciudad de la dirección de envío. */
    @Column(nullable = false, length = 120)
    private String addressCity;

    /** Código postal de la dirección de envío. */
    @Column(nullable = false, length = 20)
    private String addressPostalCode;

    /** País de la dirección de envío. */
    @Column(nullable = false, length = 120)
    private String addressCountry;

    /** Comentarios adicionales del cliente (opcional). */
    @Column(length = 500)
    private String notes;

    /** Líneas que componen el pedido. */
    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<OrderItem> items = new ArrayList<>();

    /** Suma de las líneas antes de impuestos y envío. */
    @Column(nullable = false)
    private Double subtotal;

    /** Impuestos aplicados (IVA 21%). */
    @Column(nullable = false)
    private Double tax;

    /** Gastos de envío. */
    @Column(nullable = false)
    private Double shipping;

    /** Importe total del pedido. */
    @Column(nullable = false)
    private Double total;

    /** Estado actual del pedido. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    /** Fecha y hora de creación del pedido. */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Constructor vacío requerido por JPA. */
    public Order() {
    }

    /**
     * Añade una línea al pedido manteniendo la relación bidireccional.
     *
     * @param item línea a añadir.
     */
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}
