package com.pardorps.pardrops.model;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Estados posibles por los que pasa un pedido.
 *
 * <p>El flujo habitual de preparación de un pedido es
 * {@link #PENDING} → {@link #PAID} → {@link #PREPARED} → {@link #SHIPPED} →
 * {@link #DELIVERED}. El administrador puede avanzar el estado desde el panel
 * de pedidos, saltándose los pasos que no apliquen (por ejemplo, marcar
 * directamente {@link #PREPARED} si el pago se gestiona aparte). En cualquier
 * momento anterior a la entrega se puede pasar a {@link #CANCELLED}.</p>
 *
 * <p>{@link #DELIVERED} y {@link #CANCELLED} son estados finales: una vez
 * alcanzados, el pedido ya no admite más cambios.</p>
 */
public enum OrderStatus {

    /** Pedido registrado, pendiente de gestión por parte del administrador. */
    PENDING,

    /** Pedido pagado. */
    PAID,

    /** Pedido preparado y listo para su envío. */
    PREPARED,

    /** Pedido enviado al cliente. */
    SHIPPED,

    /** Pedido entregado al cliente (estado final). */
    DELIVERED,

    /** Pedido cancelado (estado final). */
    CANCELLED;

    /**
     * Indica si el pedido ha llegado a un estado final y ya no admite cambios.
     *
     * @return {@code true} si es {@link #DELIVERED} o {@link #CANCELLED}.
     */
    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED;
    }

    /**
     * Estados a los que se puede pasar desde el estado actual.
     *
     * @return conjunto (posiblemente vacío) de estados destino permitidos.
     */
    public Set<OrderStatus> allowedTransitions() {
        return switch (this) {
            case PENDING -> EnumSet.of(PAID, PREPARED, SHIPPED, CANCELLED);
            case PAID -> EnumSet.of(PREPARED, SHIPPED, CANCELLED);
            case PREPARED -> EnumSet.of(SHIPPED, CANCELLED);
            case SHIPPED -> EnumSet.of(DELIVERED, CANCELLED);
            case DELIVERED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    /**
     * Comprueba si es un cambio de estado válido desde el estado actual.
     *
     * <p>Solo se permite avanzar en el flujo de preparación (o cancelar antes
     * de la entrega); nunca quedarse igual ni retroceder.</p>
     *
     * @param target estado al que se quiere pasar.
     * @return {@code true} si la transición está permitida.
     */
    public boolean canTransitionTo(OrderStatus target) {
        return target != null && allowedTransitions().contains(target);
    }

    /**
     * Calcula el estado global de un pedido a partir de sus partes.
     *
     * <p>El estado global refleja el peor caso: el menos avanzado de los
     * subpedidos que siguen vivos. Los subpedidos cancelados no retrasan el
     * pedido, porque el resto de la mercancía ya no depende de ellos; si todos
     * están cancelados, el pedido queda cancelado.</p>
     *
     * @param statuses estados de las partes del pedido.
     * @return el estado global, o {@code null} si no hay ninguna parte.
     */
    public static OrderStatus aggregate(Collection<OrderStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return null;
        }

        List<OrderStatus> alive = statuses.stream()
                .filter(status -> status != null && status != CANCELLED)
                .toList();

        if (alive.isEmpty()) {
            return CANCELLED;
        }

        return alive.stream()
                .min(Comparator.comparingInt(OrderStatus::progress))
                .orElse(null);
    }

    /**
     * Devuelve el nivel de avance del estado dentro del flujo de preparación.
     *
     * @return número de avance (0 es el estado inicial).
     */
    private int progress() {
        return switch (this) {
            case PENDING -> 0;
            case PAID -> 1;
            case PREPARED -> 2;
            case SHIPPED -> 3;
            case DELIVERED -> 4;
            case CANCELLED -> 5;
        };
    }
}
