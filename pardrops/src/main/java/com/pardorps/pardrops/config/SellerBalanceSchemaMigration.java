package com.pardorps.pardrops.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Añade las columnas de saldo reservado y retirado a {@code seller_balances}.
 *
 * <p>Hibernate no puede añadir columnas {@code NOT NULL} a una tabla con filas,
 * por lo que se crean aquí con valor por defecto.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SellerBalanceSchemaMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SellerBalanceSchemaMigration.class);

    private final JdbcTemplate jdbcTemplate;

    /**
     * @param jdbcTemplate acceso directo a la base de datos.
     */
    public SellerBalanceSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        String product;
        try {
            product = jdbcTemplate.execute((org.springframework.jdbc.core.ConnectionCallback<String>) connection ->
                    connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT));
        } catch (Exception exception) {
            return;
        }
        if (!product.contains("postgres")) {
            return;
        }

        try {
            jdbcTemplate.execute("ALTER TABLE seller_balances ADD COLUMN IF NOT EXISTS reserved_amount DOUBLE PRECISION NOT NULL DEFAULT 0");
            jdbcTemplate.execute("ALTER TABLE seller_balances ADD COLUMN IF NOT EXISTS withdrawn_total DOUBLE PRECISION NOT NULL DEFAULT 0");
            jdbcTemplate.execute("UPDATE seller_balances SET reserved_amount = 0 WHERE reserved_amount IS NULL");
            jdbcTemplate.execute("UPDATE seller_balances SET withdrawn_total = 0 WHERE withdrawn_total IS NULL");
            log.info("Columnas de saldo verificadas");
        } catch (Exception exception) {
            log.warn("No se pudieron preparar las columnas de saldo: {}", exception.getMessage());
        }
    }
}
