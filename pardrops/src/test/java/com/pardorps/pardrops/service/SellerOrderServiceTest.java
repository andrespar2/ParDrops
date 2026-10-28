package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.SellerOrderResponse;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.NotificationType;
import com.pardorps.pardrops.model.Order;
import com.pardorps.pardrops.model.OrderStatus;
import com.pardorps.pardrops.model.OrderSubOrder;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.OrderRepository;
import com.pardorps.pardrops.repository.OrderSubOrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la gestión de los pedidos por parte del vendedor.
 */
class SellerOrderServiceTest {

    private final OrderSubOrderRepository subOrderRepository = mock(OrderSubOrderRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final OrderEmailService orderEmailService = mock(OrderEmailService.class);
    private final SellerBalanceService sellerBalanceService = mock(SellerBalanceService.class);

    private final SellerOrderService service = new SellerOrderService(
            subOrderRepository,
            orderRepository,
            notificationService,
            orderEmailService,
            sellerBalanceService
    );

    @Test
    @DisplayName("Devuelve únicamente las partes del vendedor autenticado")
    void listsOnlySellerSubOrders() {
        User seller = seller(1L);
        OrderSubOrder subOrder = subOrder(10L, seller, OrderStatus.PENDING);
        subOrder.setOrder(order(100L));
        when(subOrderRepository.findBySellerId(1L)).thenReturn(List.of(subOrder));

        List<SellerOrderResponse> responses = service.findMine(seller);

        assertEquals(1, responses.size());
        assertEquals(10L, responses.get(0).id());
        assertEquals("Estilo Urbano", responses.get(0).storeName());
        assertEquals("Cliente de Prueba", responses.get(0).customerName());
        assertEquals("Calle 1 # 2-3", responses.get(0).addressStreet());
    }

    @Test
    @DisplayName("Avanza el estado de su parte y recalcula el estado global del pedido")
    void advancesSubOrderAndRecalculatesGlobalStatus() {
        User seller = seller(1L);
        Order order = order(100L);
        OrderSubOrder mine = subOrder(10L, seller, OrderStatus.PENDING);
        OrderSubOrder other = subOrder(11L, seller(2L), OrderStatus.PENDING);
        order.getSubOrders().add(mine);
        order.getSubOrders().add(other);
        mine.setOrder(order);
        other.setOrder(order);

        when(subOrderRepository.findByIdAndSellerId(10L, 1L)).thenReturn(Optional.of(mine));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SellerOrderResponse response = service.updateStatus(seller, 10L, OrderStatus.PREPARED);

        assertEquals(OrderStatus.PREPARED, response.status());
        // La otra tienda sigue pendiente: el estado global es el peor caso.
        assertEquals(OrderStatus.PENDING, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("El pedido queda entregado cuando todas sus partes lo están")
    void deliversOrderWhenEveryPartIsDelivered() {
        User seller = seller(1L);
        Order order = order(100L);
        OrderSubOrder mine = subOrder(10L, seller, OrderStatus.SHIPPED);
        OrderSubOrder other = subOrder(11L, seller(2L), OrderStatus.DELIVERED);
        order.getSubOrders().add(mine);
        order.getSubOrders().add(other);
        mine.setOrder(order);
        other.setOrder(order);

        when(subOrderRepository.findByIdAndSellerId(10L, 1L)).thenReturn(Optional.of(mine));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateStatus(seller, 10L, OrderStatus.DELIVERED);

        assertEquals(OrderStatus.DELIVERED, order.getStatus());
    }

    @Test
    @DisplayName("Rechaza una transición inválida con un conflicto (409)")
    void rejectsInvalidTransitionWithConflict() {
        User seller = seller(1L);
        OrderSubOrder subOrder = subOrder(10L, seller, OrderStatus.SHIPPED);
        when(subOrderRepository.findByIdAndSellerId(10L, 1L)).thenReturn(Optional.of(subOrder));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.updateStatus(seller, 10L, OrderStatus.PREPARED)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals(OrderStatus.SHIPPED, subOrder.getStatus());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Rechaza modificar una parte que ya está en un estado final")
    void rejectsChangeOnTerminalSubOrder() {
        User seller = seller(1L);
        OrderSubOrder subOrder = subOrder(10L, seller, OrderStatus.DELIVERED);
        when(subOrderRepository.findByIdAndSellerId(10L, 1L)).thenReturn(Optional.of(subOrder));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.updateStatus(seller, 10L, OrderStatus.CANCELLED)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("No deja tocar la parte de otro vendedor")
    void preventsAccessToAnotherSellersSubOrder() {
        User intruder = seller(2L);
        when(subOrderRepository.findByIdAndSellerId(10L, 2L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.updateStatus(intruder, 10L, OrderStatus.PREPARED)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getMessage().contains("10"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Avisa al cliente cuando cambia el estado de su pedido")
    void notifiesCustomerOnStatusChange() {
        User seller = seller(1L);
        User customer = customer(9L);
        Order order = order(100L);
        order.setUser(customer);
        OrderSubOrder mine = subOrder(10L, seller, OrderStatus.PENDING);
        order.getSubOrders().add(mine);
        mine.setOrder(order);

        when(subOrderRepository.findByIdAndSellerId(10L, 1L)).thenReturn(Optional.of(mine));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateStatus(seller, 10L, OrderStatus.PREPARED);

        verify(notificationService).notify(
                eq(customer), eq(NotificationType.ORDER), any(), any(), eq("/cuenta"));
        verify(orderEmailService).sendStatusUpdate(order, mine);
    }

    @Test
    @DisplayName("Envía el comprobante cuando el vendedor marca su parte como pagada")
    void sendsReceiptWhenSubOrderIsPaid() {
        User seller = seller(1L);
        Order order = order(101L);
        OrderSubOrder mine = subOrder(11L, seller, OrderStatus.PENDING);
        order.getSubOrders().add(mine);
        mine.setOrder(order);

        when(subOrderRepository.findByIdAndSellerId(11L, 1L)).thenReturn(Optional.of(mine));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateStatus(seller, 11L, OrderStatus.PAID);

        verify(orderEmailService).sendPaymentReceipt(order, mine);
    }

    @Test
    @DisplayName("Libera el saldo cuando el vendedor entrega su parte")
    void releasesFundsWhenDelivered() {
        User seller = seller(1L);
        Order order = order(102L);
        OrderSubOrder mine = subOrder(12L, seller, OrderStatus.SHIPPED);
        order.getSubOrders().add(mine);
        mine.setOrder(order);

        when(subOrderRepository.findByIdAndSellerId(12L, 1L)).thenReturn(Optional.of(mine));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateStatus(seller, 12L, OrderStatus.DELIVERED);

        verify(sellerBalanceService).release(mine);
    }

    @Test
    @DisplayName("Anula la retención cuando el vendedor cancela su parte")
    void cancelsFundsWhenCancelled() {
        User seller = seller(1L);
        Order order = order(103L);
        OrderSubOrder mine = subOrder(13L, seller, OrderStatus.PENDING);
        order.getSubOrders().add(mine);
        mine.setOrder(order);

        when(subOrderRepository.findByIdAndSellerId(13L, 1L)).thenReturn(Optional.of(mine));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateStatus(seller, 13L, OrderStatus.CANCELLED);

        verify(sellerBalanceService).cancel(mine);
    }

    /**
     * Construye un vendedor de prueba.
     *
     * @param id identificador del vendedor.
     * @return el vendedor listo para usar.
     */
    private User seller(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("vendedor" + id + "@pardrops.com");
        user.setPassword("hash");
        user.setFullName("Estilo Urbano");
        user.setRole(Role.SELLER);
        return user;
    }

    /**
     * Construye un cliente de prueba.
     *
     * @param id identificador del cliente.
     * @return el cliente listo para usar.
     */
    private User customer(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("cliente" + id + "@pardrops.com");
        user.setPassword("hash");
        user.setFullName("Cliente de Prueba");
        user.setRole(Role.CUSTOMER);
        return user;
    }

    /**
     * Construye un pedido de prueba.
     *
     * @param id identificador del pedido.
     * @return el pedido listo para usar.
     */
    private Order order(Long id) {
        Order order = new Order();
        order.setId(id);
        order.setCustomerName("Cliente de Prueba");
        order.setCustomerEmail("cliente@pardrops.com");
        order.setCustomerPhone("+57 300 000 0000");
        order.setAddressStreet("Calle 1 # 2-3");
        order.setAddressCity("Bogotá");
        order.setAddressPostalCode("110111");
        order.setAddressCountry("Colombia");
        order.setSubtotal(150_000.0);
        order.setTax(28_500.0);
        order.setShipping(25_900.0);
        order.setTotal(204_400.0);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(Instant.now());
        return order;
    }

    /**
     * Construye una parte de pedido de prueba.
     *
     * @param id     identificador de la parte.
     * @param seller vendedor responsable de la parte.
     * @param status estado de la parte.
     * @return la parte lista para usar.
     */
    private OrderSubOrder subOrder(Long id, User seller, OrderStatus status) {
        OrderSubOrder subOrder = new OrderSubOrder();
        subOrder.setId(id);
        subOrder.setSeller(seller);
        subOrder.setStoreName("Estilo Urbano");
        subOrder.setStoreSlug("estilo-urbano");
        subOrder.setStatus(status);
        subOrder.setSubtotal(100_000.0);
        subOrder.setShipping(12_950.0);
        subOrder.setTotal(131_950.0);
        subOrder.setCreatedAt(Instant.now());
        return subOrder;
    }
}
