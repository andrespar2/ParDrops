package com.pardorps.pardrops.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Completa el esquema financiero y reconstruye los saldos históricos.
 *
 * <p>Hibernate crea las columnas nuevas, pero los subpedidos anteriores quedan
 * sin comisión ni estado de fondos. Esta migración:</p>
 *
 * <ol>
 *   <li>Rellena la tasa, la comisión, el neto y el estado de los subpedidos
 *       históricos.</li>
 *   <li>Reconstruye los saldos de los vendedores una sola vez, cuando la tabla
 *       de saldos está vacía.</li>
 * </ol>
 *
 * <p>Es idempotente: los backfills usan {@code IS NULL} y la reconstrucción de
 * saldos solo ocurre si no hay ninguno.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 25)
public class FinanceSchemaMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(FinanceSchemaMigration.class);

    private final JdbcTemplate jdbcTemplate;
    private final double commissionRate;

    /**
     * @param jdbcTemplate   acceso directo a la base de datos.
     * @param commissionRate tasa de comisión configurada.
     */
    public FinanceSchemaMigration(
            JdbcTemplate jdbcTemplate,
            @Value("${app.marketplace.commission-rate:0.05}") double commissionRate
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.commissionRate = commissionRate;
    }

    @Override
    public void run(String... args) {
        if (!columnExists("ORDER_SUB_ORDERS", "COMMISSION_RATE")) {
            return;
        }
        backfillFinancialFields();
        rebuildBalancesIfEmpty();
    }

    /**
     * Rellena los campos financieros de los subpedidos antiguos.
     */
    private void backfillFinancialFields() {
        int updated = jdbcTemplate.update("""
                UPDATE order_sub_orders
                SET commission_rate = ?,
                    commission_amount = ROUND(subtotal * ?, 2),
                    seller_net_amount = ROUND(total - ROUND(subtotal * ?, 2), 2),
                    funds_status = CASE
                        WHEN status = 'DELIVERED' THEN 'RELEASED'
                        WHEN status = 'CANCELLED' THEN 'CANCELLED'
                        ELSE 'HELD'
                    END
                WHERE commission_rate IS NULL AND seller_id IS NOT NULL
                """, commissionRate, commissionRate, commissionRate);
        if (updated > 0) {
            log.info("Subpedidos históricos con comisión calculada: {}", updated);
        }
    }

    /**
     * Reconstruye los saldos desde los subpedidos cuando la tabla está vacía.
     */
    private void rebuildBalancesIfEmpty() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM seller_balances", Integer.class);
        if (count != null && count > 0) {
            return;
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT seller_id,
                       COALESCE(SUM(CASE WHEN funds_status = 'HELD' THEN seller_net_amount ELSE 0 END), 0) AS pending,
                       COALESCE(SUM(CASE WHEN funds_status = 'RELEASED' THEN seller_net_amount ELSE 0 END), 0) AS available,
                       COALESCE(SUM(CASE WHEN funds_status <> 'CANCELLED' THEN commission_amount ELSE 0 END), 0) AS commission,
                       COALESCE(SUM(CASE WHEN funds_status <> 'CANCELLED' THEN subtotal ELSE 0 END), 0) AS sales
                FROM order_sub_orders
                WHERE seller_id IS NOT NULL
                GROUP BY seller_id
                """);

        for (Map<String, Object> row : rows) {
            jdbcTemplate.update("""
                    INSERT INTO seller_balances
                        (seller_id, pending_amount, available_amount, commission_total, total_sales, updated_at)
                    VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                    """,
                    row.get("SELLER_ID"),
                    row.get("PENDING"),
                    row.get("AVAILABLE"),
                    row.get("COMMISSION"),
                    row.get("SALES"));
        }
        if (!rows.isEmpty()) {
            log.info("Saldos de vendedores reconstruidos: {}", rows.size());
        }
    }

    /** Comprueba si una columna existe. */
    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = ? AND COLUMN_NAME = ?
                """, Integer.class, table, column);
        return count != null && count > 0;
    }
}
