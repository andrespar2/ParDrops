package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.CreateOrderRequest;
import com.pardorps.pardrops.dto.OrderItemRequest;
import com.pardorps.pardrops.dto.OrderResponse;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.NotificationType;
import com.pardorps.pardrops.model.Order;
import com.pardorps.pardrops.model.OrderStatus;
import com.pardorps.pardrops.model.OrderSubOrder;
import com.pardorps.pardrops.model.Product;
import com.pardorps.pardrops.model.ProductVariant;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.Store;
import com.pardorps.pardrops.model.StoreStatus;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.OrderRepository;
import com.pardorps.pardrops.repository.ProductRepository;
import com.pardorps.pardrops.repository.StoreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la lógica de pedidos, incluida la división en subpedidos por
 * vendedor.
 */
class OrderServiceTest {

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final StoreRepository storeRepository = mock(StoreRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final OrderEmailService orderEmailService = mock(OrderEmailService.class);
    private final SellerBalanceService sellerBalanceService = mock(SellerBalanceService.class);

    private final OrderService orderService = new OrderService(
            orderRepository,
            productRepository,
            storeRepository,
            notificationService,
            orderEmailService,
            sellerBalanceService,
            0.05
    );

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

    @Test
    @DisplayName("Una compra con dos vendedores genera dos subpedidos con sus importes")
    void createsOneSubOrderPerSeller() {
        User sellerA = seller(1L, "Estilo Urbano");
        User sellerB = seller(2L, "Calzado Deportivo");
        Product productA = product("prod-a", "Gorra urbana", 100_000.0, 10, sellerA);
        Product productB = product("prod-b", "Zapatillas", 50_000.0, 8, sellerB);

        when(productRepository.findById("prod-a")).thenReturn(Optional.of(productA));
        when(productRepository.findById("prod-b")).thenReturn(Optional.of(productB));
        when(storeRepository.findBySellerId(1L)).thenReturn(Optional.of(store(sellerA, "Estilo Urbano", "estilo-urbano")));
        when(storeRepository.findBySellerId(2L)).thenReturn(Optional.of(store(sellerB, "Calzado Deportivo", "calzado-deportivo")));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.createOrder(
                request(List.of(item("prod-a", 1), item("prod-b", 1))),
                null);

        assertEquals(2, response.subOrders().size());
        assertEquals("Estilo Urbano", response.subOrders().get(0).storeName());
        assertEquals("Calzado Deportivo", response.subOrders().get(1).storeName());
        assertEquals(100_000.0, response.subOrders().get(0).subtotal(), 0.001);
        assertEquals(50_000.0, response.subOrders().get(1).subtotal(), 0.001);

        // Los impuestos se reparten en proporción al importe de cada parte y el
        // envío a partes iguales: la suma coincide con el total del pedido.
        assertEquals(19_000.0, response.subOrders().get(0).total() - 100_000.0 - 12_950.0, 0.001);
        assertEquals(9_500.0, response.subOrders().get(1).total() - 50_000.0 - 12_950.0, 0.001);
        assertEquals(12_950.0, response.subOrders().get(0).shipping(), 0.001);
        assertEquals(12_950.0, response.subOrders().get(1).shipping(), 0.001);

        double suma = response.subOrders().stream()
                .mapToDouble(subOrder -> subOrder.total())
                .sum();
        assertEquals(response.total(), suma, 0.001);
    }

    @Test
    @DisplayName("Los productos sin vendedor forman la parte de la tienda propia")
    void createsPlatformSubOrderForProductsWithoutSeller() {
        Product product = product("prod-plat", "Camiseta", 30_000.0, 5, null);
        when(productRepository.findById("prod-plat")).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.createOrder(request(List.of(item("prod-plat", 1))), null);

        assertEquals(1, response.subOrders().size());
        assertEquals("ParDrops", response.subOrders().get(0).storeName());
        assertNull(response.subOrders().get(0).sellerName());
    }

    @Test
    @DisplayName("Descuenta el stock una sola vez por unidad comprada")
    void decreasesStockOncePerUnit() {
        User seller = seller(1L, "Estilo Urbano");
        Product product = product("prod-a", "Gorra urbana", 100_000.0, 10, seller);
        when(productRepository.findById("prod-a")).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.createOrder(request(List.of(item("prod-a", 3))), null);

        assertEquals(7, product.getStock());
        verify(productRepository, times(1)).save(product);
    }

    @Test
    @DisplayName("No descuenta stock ni guarda el pedido si no hay inventario suficiente")
    void doesNotSaveOrderWhenStockIsInsufficient() {
        User seller = seller(1L, "Estilo Urbano");
        Product product = product("prod-a", "Gorra urbana", 100_000.0, 2, seller);
        when(productRepository.findById("prod-a")).thenReturn(Optional.of(product));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> orderService.createOrder(request(List.of(item("prod-a", 5))), null)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals(2, product.getStock());
        verify(productRepository, never()).save(any(Product.class));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Rechaza comprar un producto que ya no está disponible")
    void rejectsInactiveProduct() {
        Product product = product("prod-a", "Gorra urbana", 100_000.0, 10, null);
        product.setActive(false);
        when(productRepository.findById("prod-a")).thenReturn(Optional.of(product));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> orderService.createOrder(request(List.of(item("prod-a", 1))), null)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Avisa a cada vendedor de su parte y al cliente del pedido")
    void notifiesSellersAndCustomer() {
        User sellerA = seller(1L, "Estilo Urbano");
        User sellerB = seller(2L, "Calzado Deportivo");
        User customer = customer(9L);
        Product productA = product("prod-a", "Gorra urbana", 100_000.0, 10, sellerA);
        Product productB = product("prod-b", "Zapatillas", 50_000.0, 8, sellerB);

        when(productRepository.findById("prod-a")).thenReturn(Optional.of(productA));
        when(productRepository.findById("prod-b")).thenReturn(Optional.of(productB));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.createOrder(
                request(List.of(item("prod-a", 1), item("prod-b", 1))),
                customer);

        verify(notificationService).notify(
                eq(sellerA), eq(NotificationType.ORDER), any(), any(), eq("/vendedor/pedidos"));
        verify(notificationService).notify(
                eq(sellerB), eq(NotificationType.ORDER), any(), any(), eq("/vendedor/pedidos"));
        verify(notificationService).notify(
                eq(customer), eq(NotificationType.ORDER), any(), any(), eq("/cuenta"));
        verify(orderEmailService).sendNewOrderToSellers(any(Order.class));
    }

    @Test
    @DisplayName("Los pedidos antiguos sin subpedidos siguen mostrándose igual")
    void keepsLegacyOrdersWithoutSubOrders() {
        Order order = order(OrderStatus.PENDING);
        when(orderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(order));

        List<OrderResponse> orders = orderService.getAllOrders();

        assertEquals(1, orders.size());
        assertEquals(0, orders.get(0).subOrders().size());
        assertEquals(OrderStatus.PENDING, orders.get(0).status());
    }

    /**
     * Construye un pedido de prueba con el estado indicado.
     *
     * @param status estado inicial.
     * @return el pedido listo para usar en las pruebas.
     */
    private Order order(OrderStatus status) {
        Order order = new Order();
        order.setId(1L);
        order.setCustomerName("Cliente de Prueba");
        order.setCustomerEmail("cliente@pardrops.com");
        order.setCustomerPhone("+57 300 111 2222");
        order.setAddressStreet("Carrera 7 # 12-45");
        order.setAddressCity("Bogotá");
        order.setAddressPostalCode("110111");
        order.setAddressCountry("Colombia");
        order.setSubtotal(10.0);
        order.setTax(1.9);
        order.setShipping(25900.0);
        order.setTotal(25911.9);
        order.setStatus(status);
        order.setCreatedAt(Instant.now());
        return order;
    }

    /**
     * Construye una petición de compra con las líneas indicadas.
     *
     * @param items líneas del pedido.
     * @return la petición lista para usar.
     */
    private CreateOrderRequest request(List<OrderItemRequest> items) {
        return new CreateOrderRequest(
                "Cliente de Prueba",
                "cliente@pardrops.com",
                "+57 300 000 0000",
                "Calle 1 # 2-3",
                "Bogotá",
                "110111",
                "Colombia",
                null,
                items
        );
    }

    /**
     * Construye una línea de pedido.
     *
     * @param productId producto comprado.
     * @param quantity  unidades compradas.
     * @return la línea lista para usar.
     */
    private OrderItemRequest item(String productId, int quantity) {
        return new OrderItemRequest(productId, null, null, quantity);
    }

    /**
     * Construye un producto del catálogo.
     *
     * @param id     identificador del producto.
     * @param name   nombre del producto.
     * @param price  precio unitario.
     * @param stock  unidades disponibles.
     * @param seller vendedor del producto (nulo si es de la tienda propia).
     * @return el producto listo para usar.
     */
    private Product product(String id, String name, double price, int stock, User seller) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setSlug(id);
        product.setDescription("Descripción de prueba");
        product.setPrice(price);
        product.setStock(stock);
        product.setSeller(seller);
        product.setActive(true);
        return product;
    }

    @Test
    @DisplayName("Envía el comprobante cuando el pedido pasa a pagado")
    void sendsReceiptWhenOrderIsPaid() {
        Order order = new Order();
        order.setId(42L);
        order.setStatus(OrderStatus.PENDING);
        order.setCustomerEmail("cliente@pardrops.com");
        when(orderRepository.findById(42L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        orderService.updateStatus(42L, OrderStatus.PAID);

        verify(orderEmailService).sendPaymentReceipt(order);
    }

    @Test
    @DisplayName("Envía el aviso y notifica al cliente cuando el pedido avanza")
    void sendsStatusEmailWhenOrderAdvances() {
        User customer = customer(10L);
        Order order = new Order();
        order.setId(43L);
        order.setStatus(OrderStatus.PENDING);
        order.setCustomerEmail("cliente@pardrops.com");
        order.setUser(customer);
        when(orderRepository.findById(43L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        orderService.updateStatus(43L, OrderStatus.PREPARED);

        verify(orderEmailService).sendStatusUpdate(order);
        verify(notificationService).notify(
                eq(customer), eq(NotificationType.ORDER), any(), any(), eq("/cuenta"));
    }

    @Test
    @DisplayName("Calcula la comisión y retiene el saldo del vendedor")
    void calculatesCommissionAndHoldsFunds() {
        User seller = seller(3L, "Tienda Comisión");
        Product product = product("prod-c", "Gorra", 100_000.0, 10, seller);
        when(productRepository.findById("prod-c")).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.createOrder(request(List.of(item("prod-c", 1))), customer(9L));

        org.mockito.ArgumentCaptor<Order> captor =
                org.mockito.ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        OrderSubOrder subOrder = captor.getValue().getSubOrders().get(0);

        assertEquals(0.05, subOrder.getCommissionRate());
        assertEquals(5_000.0, subOrder.getCommissionAmount());
        assertEquals(139_900.0, subOrder.getSellerNetAmount());
        assertEquals(com.pardorps.pardrops.model.FundsStatus.HELD, subOrder.getFundsStatus());
        verify(sellerBalanceService).hold(subOrder);
    }

    @Test
    @DisplayName("Usa el precio y el stock de la variante elegida")
    void usesVariantPriceAndStock() {
        User seller = seller(4L, "Tienda Variantes");
        Product product = product("prod-v", "Camiseta", 100_000.0, 10, seller);
        product.getVariants().add(new ProductVariant(
                "v-m", "Talla M", 120_000.0, 2, "SKU-M", "M", null, null));
        when(productRepository.findById("prod-v")).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.createOrder(
                request(List.of(new OrderItemRequest("prod-v", null, "v-m", null, 1))),
                customer(9L));

        assertEquals(120_000.0, response.items().get(0).unitPrice());
        assertEquals(1, product.getVariants().get(0).getStock());
        assertEquals(9, product.getStock());
    }

    @Test
    @DisplayName("Rechaza la compra si la variante no tiene stock")
    void rejectsOutOfStockVariant() {
        User seller = seller(5L, "Tienda Variantes");
        Product product = product("prod-v2", "Camiseta", 100_000.0, 10, seller);
        product.getVariants().add(new ProductVariant(
                "v-s", "Talla S", 100_000.0, 1, "SKU-S", "S", null, null));
        when(productRepository.findById("prod-v2")).thenReturn(Optional.of(product));

        ApiException error = assertThrows(ApiException.class,
                () -> orderService.createOrder(
                        request(List.of(new OrderItemRequest("prod-v2", null, "v-s", null, 2))),
                        customer(9L)));

        assertEquals(HttpStatus.CONFLICT, error.getStatus());
    }

    /**
     * Construye un vendedor de prueba.
     *
     * @param id    identificador del vendedor.
     * @param name  nombre comercial.
     * @return el vendedor listo para usar.
     */
    private User seller(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setEmail("vendedor" + id + "@pardrops.com");
        user.setPassword("hash");
        user.setFullName(name);
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
     * Construye una tienda de prueba.
     *
     * @param seller propietario de la tienda.
     * @param name   nombre comercial de la tienda.
     * @param slug   slug público de la tienda.
     * @return la tienda lista para usar.
     */
    private Store store(User seller, String name, String slug) {
        Store store = new Store();
        store.setSeller(seller);
        store.setName(name);
        store.setSlug(slug);
        store.setStatus(StoreStatus.ACTIVE);
        store.setCreatedAt(Instant.now());
        return store;
    }
}
