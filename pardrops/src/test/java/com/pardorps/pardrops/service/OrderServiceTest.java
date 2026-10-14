package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.OrderResponse;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.Order;
import com.pardorps.pardrops.model.OrderStatus;
import com.pardorps.pardrops.repository.OrderRepository;
import com.pardorps.pardrops.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas del cambio de estado de un pedido en {@link OrderService}.
 */
class OrderServiceTest {

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final OrderService orderService = new OrderService(orderRepository, productRepository);

    @Test
    @DisplayName("Actualiza el estado cuando la transición es válida")
    void updatesStatusWhenTransitionIsValid() {
        Order order = order(OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.updateStatus(1L, OrderStatus.PREPARED);

        assertEquals(OrderStatus.PREPARED, response.status());
        assertEquals(OrderStatus.PREPARED, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("Rechaza un retroceso de estado con un conflicto (409)")
    void rejectsInvalidTransitionWithConflict() {
        Order order = order(OrderStatus.SHIPPED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> orderService.updateStatus(1L, OrderStatus.PENDING)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals(OrderStatus.SHIPPED, order.getStatus());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Rechaza modificar un pedido ya entregado")
    void rejectsChangeOnDeliveredOrder() {
        Order order = order(OrderStatus.DELIVERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> orderService.updateStatus(1L, OrderStatus.CANCELLED)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Devuelve 404 si el pedido no existe")
    void failsWhenOrderDoesNotExist() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> orderService.updateStatus(99L, OrderStatus.PREPARED)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getMessage().contains("99"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    /**
     * Crea un pedido de prueba con el estado indicado.
     *
     * @param status estado inicial.
     * @return el pedido listo para usar en las pruebas.
     */
    private Order order(OrderStatus status) {
        Order order = new Order();
        order.setId(1L);
        order.setCustomerName("Cliente de Prueba");
        order.setCustomerEmail("cliente@pardrops.com");
        order.setCustomerPhone("+34 600 111 222");
        order.setAddressStreet("Calle Mayor 1");
        order.setAddressCity("Madrid");
        order.setAddressPostalCode("28001");
        order.setAddressCountry("España");
        order.setSubtotal(10.0);
        order.setTax(2.1);
        order.setShipping(5.99);
        order.setTotal(18.09);
        order.setStatus(status);
        order.setCreatedAt(Instant.now());
        return order;
    }
}
