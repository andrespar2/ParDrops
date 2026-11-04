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
 * Prepara la configuración de envío por producto en PostgreSQL.
 *
 * <p>Hibernate añade las columnas nuevas. Esta migración las crea de forma
 * idempotente y copia una sola vez la configuración que antes vivía en la
 * tienda, para que los productos existentes conserven su envío hasta que el
 * vendedor lo edite.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class ProductShippingMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductShippingMigration.class);

    private final JdbcTemplate jdbcTemplate;

    /**
     * @param jdbcTemplate acceso directo a la base de datos.
     */
    public ProductShippingMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        String product;
        try {
            product = jdbcTemplate.execute(
                    (org.springframework.jdbc.core.ConnectionCallback<String>) connection ->
                            connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT));
        } catch (Exception exception) {
            log.debug("No se pudo detectar el motor de base de datos: {}", exception.getMessage());
            return;
        }

        if (product == null || !product.contains("postgres")) {
            return;
        }

        try {
            jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS requires_shipping BOOLEAN");
            jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS shipping_cost DOUBLE PRECISION");
            jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS free_shipping_threshold DOUBLE PRECISION");
            jdbcTemplate.execute("UPDATE products SET requires_shipping = false WHERE requires_shipping IS NULL");
            int copied = jdbcTemplate.update("""
                    UPDATE products p
                    SET requires_shipping = true,
                        shipping_cost = s.shipping_cost,
                        free_shipping_threshold = s.free_shipping_threshold
                    FROM stores s
                    WHERE s.seller_id = p.seller_id
                      AND p.requires_shipping = false
                      AND p.shipping_cost IS NULL
                      AND s.shipping_cost IS NOT NULL
                    """);
            if (copied > 0) {
                log.info("Configuración de envío copiada a {} productos", copied);
            }
            log.info("Columnas de envío por producto verificadas");
        } catch (Exception exception) {
            log.warn("No se pudieron preparar las columnas de envío: {}", exception.getMessage());
        }
    }
}
