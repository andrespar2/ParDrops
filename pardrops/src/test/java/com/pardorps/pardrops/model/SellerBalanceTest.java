package com.pardorps.pardrops.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Pruebas del saldo del vendedor.
 */
class SellerBalanceTest {

    @Test
    @DisplayName("El saldo empieza en cero y se inicializa al persistir")
    void initializesAmounts() {
        SellerBalance balance = new SellerBalance();
        balance.setSeller(User.builder().id(1L).build());

        balance.onCreate();

        assertEquals(0.0, balance.getPendingAmount());
        assertEquals(0.0, balance.getAvailableAmount());
        assertEquals(0.0, balance.getCommissionTotal());
        assertEquals(0.0, balance.getTotalSales());
        assertNotNull(balance.getUpdatedAt());
    }

    @Test
    @DisplayName("Los importes se actualizan y la fecha cambia")
    void updatesAmounts() {
        SellerBalance balance = new SellerBalance();
        balance.setPendingAmount(139_900.0);
        balance.setAvailableAmount(50_000.0);
        balance.setCommissionTotal(5_000.0);
        balance.setTotalSales(100_000.0);

        balance.onUpdate();

        assertEquals(139_900.0, balance.getPendingAmount());
        assertNotNull(balance.getUpdatedAt());
    }
}
