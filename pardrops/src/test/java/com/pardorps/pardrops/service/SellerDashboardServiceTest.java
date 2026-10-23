package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.SellerDashboardResponse;
import com.pardorps.pardrops.model.OrderStatus;
import com.pardorps.pardrops.model.OrderSubOrder;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.Store;
import com.pardorps.pardrops.model.StoreStatus;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.OrderSubOrderRepository;
import com.pardorps.pardrops.repository.ProductRepository;
import com.pardorps.pardrops.repository.StoreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas del cálculo de indicadores del panel del vendedor.
 */
class SellerDashboardServiceTest {

    private final OrderSubOrderRepository subOrderRepository =
            mock(OrderSubOrderRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final StoreRepository storeRepository = mock(StoreRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);

    private final SellerDashboardService service = new SellerDashboardService(
            subOrderRepository, productRepository, storeRepository, notificationService);

    @Test
    @DisplayName("Calcula las ventas, los pedidos y los ingresos del vendedor")
    void calculatesSalesOrdersAndRevenue() {
        User seller = seller(1L);
        Store store = store(seller);

        when(subOrderRepository.findBySellerId(1L)).thenReturn(List.of(
                subOrder(OrderStatus.DELIVERED, 100.0),
                subOrder(OrderStatus.PREPARED, 80.0),
                subOrder(OrderStatus.CANCELLED, 50.0)
        ));
        when(productRepository.countPublishedBySellerId(1L)).thenReturn(6L);
        when(productRepository.countOutOfStockBySellerId(1L)).thenReturn(2L);
        when(storeRepository.findBySellerId(1L)).thenReturn(Optional.of(store));
        when(notificationService.findMine(seller)).thenReturn(List.of());

        SellerDashboardResponse summary = service.loadSummary(seller);

        assertEquals("Estilo Urbano", summary.storeName());
        assertEquals("estilo-urbano", summary.storeSlug());
        assertEquals(1L, summary.salesCount());
        assertEquals(3L, summary.orderCount());
        assertEquals(1L, summary.pendingOrderCount());
        assertEquals(6L, summary.publishedProductCount());
        assertEquals(2L, summary.outOfStockCount());
        assertEquals(180.0, summary.revenue());
    }

    @Test
    @DisplayName("Consulta siempre los datos del vendedor autenticado")
    void queriesOnlyTheAuthenticatedSeller() {
        User seller = seller(7L);

        when(subOrderRepository.findBySellerId(7L)).thenReturn(List.of());
        when(productRepository.countPublishedBySellerId(7L)).thenReturn(0L);
        when(productRepository.countOutOfStockBySellerId(7L)).thenReturn(0L);
        when(storeRepository.findBySellerId(7L)).thenReturn(Optional.empty());
        when(notificationService.findMine(seller)).thenReturn(List.of());

        SellerDashboardResponse summary = service.loadSummary(seller);

        assertEquals(0L, summary.orderCount());
        assertNull(summary.storeName());
        assertNull(summary.storeSlug());
        assertTrue(summary.recentNotifications().isEmpty());
        verify(subOrderRepository).findBySellerId(7L);
        verify(productRepository).countPublishedBySellerId(7L);
        verify(productRepository).countOutOfStockBySellerId(7L);
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
        user.setFullName("Vendedor " + id);
        user.setRole(Role.SELLER);
        return user;
    }

    /**
     * Construye la tienda de un vendedor.
     *
     * @param seller vendedor propietario.
     * @return la tienda lista para usar.
     */
    private Store store(User seller) {
        Store store = new Store();
        store.setId(1L);
        store.setSeller(seller);
        store.setName("Estilo Urbano");
        store.setSlug("estilo-urbano");
        store.setStatus(StoreStatus.ACTIVE);
        return store;
    }

    /**
     * Construye una parte de pedido con su estado e importe.
     *
     * @param status estado del subpedido.
     * @param total  importe total.
     * @return el subpedido listo para usar.
     */
    private OrderSubOrder subOrder(OrderStatus status, double total) {
        OrderSubOrder subOrder = new OrderSubOrder();
        subOrder.setStatus(status);
        subOrder.setTotal(total);
        return subOrder;
    }
}
