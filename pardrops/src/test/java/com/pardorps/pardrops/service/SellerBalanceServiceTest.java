package com.pardorps.pardrops.service;

import com.pardorps.pardrops.model.FundsStatus;
import com.pardorps.pardrops.model.OrderSubOrder;
import com.pardorps.pardrops.model.SellerBalance;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.OrderSubOrderRepository;
import com.pardorps.pardrops.repository.SellerBalanceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas del saldo retenido y liberado.
 */
class SellerBalanceServiceTest {

    private final SellerBalanceRepository balanceRepository = mock(SellerBalanceRepository.class);
    private final OrderSubOrderRepository subOrderRepository = mock(OrderSubOrderRepository.class);
    private final SellerBalanceService service = new SellerBalanceService(
            balanceRepository, subOrderRepository);

    @Test
    @DisplayName("Al crear la venta retiene el neto y suma las ventas")
    void holdsNetAmount() {
        User seller = seller();
        OrderSubOrder subOrder = subOrder(seller);
        when(balanceRepository.findBySellerId(1L)).thenReturn(Optional.empty());

        service.hold(subOrder);

        verify(balanceRepository).save(org.mockito.ArgumentMatchers.argThat(balance ->
                balance.getPendingAmount() == 139_900.0
                        && balance.getTotalSales() == 100_000.0
                        && balance.getAvailableAmount() == 0.0));
        assertEquals(FundsStatus.HELD, subOrder.getFundsStatus());
    }

    @Test
    @DisplayName("Al entregar mueve el neto a disponible y acumula la comisión")
    void releasesNetAmount() {
        User seller = seller();
        OrderSubOrder subOrder = subOrder(seller);
        subOrder.setFundsStatus(FundsStatus.HELD);
        SellerBalance balance = balance(seller);
        balance.setPendingAmount(139_900.0);
        when(balanceRepository.findBySellerId(1L)).thenReturn(Optional.of(balance));

        service.release(subOrder);

        assertEquals(0.0, balance.getPendingAmount());
        assertEquals(139_900.0, balance.getAvailableAmount());
        assertEquals(5_000.0, balance.getCommissionTotal());
        assertEquals(FundsStatus.RELEASED, subOrder.getFundsStatus());
        assertNotNull(subOrder.getReleasedAt());
    }

    @Test
    @DisplayName("No libera dos veces la misma parte")
    void releaseIsIdempotent() {
        User seller = seller();
        OrderSubOrder subOrder = subOrder(seller);
        subOrder.setFundsStatus(FundsStatus.RELEASED);

        service.release(subOrder);

        verify(balanceRepository, never()).save(any(SellerBalance.class));
    }

    @Test
    @DisplayName("Al cancelar anula la retención")
    void cancelsHold() {
        User seller = seller();
        OrderSubOrder subOrder = subOrder(seller);
        subOrder.setFundsStatus(FundsStatus.HELD);
        SellerBalance balance = balance(seller);
        balance.setPendingAmount(139_900.0);
        balance.setTotalSales(100_000.0);
        when(balanceRepository.findBySellerId(1L)).thenReturn(Optional.of(balance));

        service.cancel(subOrder);

        assertEquals(0.0, balance.getPendingAmount());
        assertEquals(0.0, balance.getTotalSales());
        assertEquals(FundsStatus.CANCELLED, subOrder.getFundsStatus());
    }

    @Test
    @DisplayName("Los subpedidos de plataforma no generan saldo")
    void skipsPlatformSubOrder() {
        OrderSubOrder subOrder = subOrder(null);

        service.hold(subOrder);
        service.release(subOrder);
        service.cancel(subOrder);

        verify(balanceRepository, never()).save(any(SellerBalance.class));
    }

    private OrderSubOrder subOrder(User seller) {
        OrderSubOrder subOrder = new OrderSubOrder();
        subOrder.setId(5L);
        subOrder.setSeller(seller);
        subOrder.setSubtotal(100_000.0);
        subOrder.setShipping(25_900.0);
        subOrder.setTotal(144_900.0);
        subOrder.setCommissionRate(0.05);
        subOrder.setCommissionAmount(5_000.0);
        subOrder.setSellerNetAmount(139_900.0);
        return subOrder;
    }

    private SellerBalance balance(User seller) {
        SellerBalance balance = new SellerBalance();
        balance.setSeller(seller);
        return balance;
    }

    private User seller() {
        return User.builder().id(1L).email("vendedor@pardrops.com").fullName("Vendedor").build();
    }
}
