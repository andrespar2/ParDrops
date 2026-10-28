package com.pardorps.pardrops.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de la migración del esquema financiero.
 */
@SpringBootTest
class FinanceSchemaMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FinanceSchemaMigration migration;

    @Test
    @DisplayName("Las columnas financieras existen en los subpedidos")
    void financialColumnsExist() {
        Integer columns = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'ORDER_SUB_ORDERS'
                  AND COLUMN_NAME IN ('COMMISSION_RATE', 'COMMISSION_AMOUNT',
                                      'SELLER_NET_AMOUNT', 'FUNDS_STATUS', 'RELEASED_AT')
                """, Integer.class);

        assertEquals(5, columns);
    }

    @Test
    @DisplayName("La tabla de saldos existe y la migración es idempotente")
    void balanceTableAndIdempotency() {
        Integer tables = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_NAME = 'SELLER_BALANCES'
                """, Integer.class);
        assertTrue(tables != null && tables > 0);

        migration.run();
        migration.run();
    }
}
