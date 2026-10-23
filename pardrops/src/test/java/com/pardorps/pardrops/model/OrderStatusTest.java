package com.pardorps.pardrops.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del flujo de estados de un pedido.
 *
 * <p>Comprueban que solo se permiten avances coherentes en el proceso de
 * preparación y que los estados finales no admiten más cambios.</p>
 */
class OrderStatusTest {

    @Test
    @DisplayName("Un pedido pendiente puede avanzar a cualquier etapa intermedia o cancelarse")
    void pendingCanAdvanceOrBeCancelled() {
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.PAID));
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.PREPARED));
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.SHIPPED));
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.CANCELLED));
    }

    @Test
    @DisplayName("Un pedido pendiente no puede marcarse como entregado directamente")
    void pendingCannotJumpToDelivered() {
        assertFalse(OrderStatus.PENDING.canTransitionTo(OrderStatus.DELIVERED));
    }

    @Test
    @DisplayName("Un estado no puede volver a un estado anterior ni repetirse")
    void cannotGoBackwardsNorRepeat() {
        assertFalse(OrderStatus.PREPARED.canTransitionTo(OrderStatus.PAID));
        assertFalse(OrderStatus.PREPARED.canTransitionTo(OrderStatus.PENDING));
        assertFalse(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.PREPARED));
        assertFalse(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.SHIPPED));
    }

    @Test
    @DisplayName("Un pedido enviado puede entregarse o cancelarse")
    void shippedCanBeDeliveredOrCancelled() {
        assertTrue(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DELIVERED));
        assertTrue(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.CANCELLED));
    }

    @Test
    @DisplayName("Entregado y cancelado son estados finales")
    void deliveredAndCancelledAreTerminal() {
        assertTrue(OrderStatus.DELIVERED.isTerminal());
        assertTrue(OrderStatus.CANCELLED.isTerminal());

        for (OrderStatus status : OrderStatus.values()) {
            assertFalse(OrderStatus.DELIVERED.canTransitionTo(status));
            assertFalse(OrderStatus.CANCELLED.canTransitionTo(status));
        }
    }

    @Test
    @DisplayName("Los estados intermedios no son finales")
    void intermediateStatusesAreNotTerminal() {
        assertFalse(OrderStatus.PENDING.isTerminal());
        assertFalse(OrderStatus.PAID.isTerminal());
        assertFalse(OrderStatus.PREPARED.isTerminal());
        assertFalse(OrderStatus.SHIPPED.isTerminal());
    }

    @Test
    @DisplayName("No se admite una transición hacia un estado nulo")
    void cannotTransitionToNull() {
        assertFalse(OrderStatus.PENDING.canTransitionTo(null));
    }

    @Test
    @DisplayName("Sin partes no hay estado global que calcular")
    void aggregateWithoutPartsIsNull() {
        assertNull(OrderStatus.aggregate(List.of()));
        assertNull(OrderStatus.aggregate(null));
    }

    @Test
    @DisplayName("El estado global refleja el peor caso entre las partes")
    void aggregateReflectsWorstCase() {
        assertEquals(OrderStatus.PENDING,
                OrderStatus.aggregate(List.of(OrderStatus.SHIPPED, OrderStatus.PENDING)));
        assertEquals(OrderStatus.PAID,
                OrderStatus.aggregate(List.of(OrderStatus.DELIVERED, OrderStatus.PAID)));
        assertEquals(OrderStatus.PREPARED,
                OrderStatus.aggregate(List.of(OrderStatus.PREPARED, OrderStatus.SHIPPED)));
    }

    @Test
    @DisplayName("Una parte cancelada no frena el avance del pedido")
    void cancelledPartDoesNotHoldBackTheOrder() {
        assertEquals(OrderStatus.SHIPPED,
                OrderStatus.aggregate(List.of(OrderStatus.CANCELLED, OrderStatus.SHIPPED)));
        assertEquals(OrderStatus.DELIVERED,
                OrderStatus.aggregate(List.of(OrderStatus.CANCELLED, OrderStatus.DELIVERED)));
    }

    @Test
    @DisplayName("Si todas las partes están canceladas, el pedido queda cancelado")
    void aggregateIsCancelledWhenEveryPartIsCancelled() {
        assertEquals(OrderStatus.CANCELLED,
                OrderStatus.aggregate(List.of(OrderStatus.CANCELLED, OrderStatus.CANCELLED)));
    }

    @Test
    @DisplayName("Una sola parte define el estado global del pedido")
    void aggregateWithSinglePartKeepsItsStatus() {
        assertEquals(OrderStatus.SHIPPED,
                OrderStatus.aggregate(List.of(OrderStatus.SHIPPED)));
        assertEquals(OrderStatus.DELIVERED,
                OrderStatus.aggregate(List.of(OrderStatus.DELIVERED)));
    }
}
