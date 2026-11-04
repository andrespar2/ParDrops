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
 * Completa las columnas de pago de los pedidos en PostgreSQL.
 *
 * <p>Hibernate añade las columnas nuevas, pero un índice único y los valores
 * por defecto de una tabla ya poblada se crean aquí de forma idempotente.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PaymentSchemaMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PaymentSchemaMigration.class);

    private final JdbcTemplate jdbcTemplate;

    /**
     * @param jdbcTemplate acceso directo a la base de datos.
     */
    public PaymentSchemaMigration(JdbcTemplate jdbcTemplate) {
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
            jdbcTemplate.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_reference VARCHAR(80)");
            jdbcTemplate.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_status VARCHAR(20)");
            jdbcTemplate.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_method VARCHAR(40)");
            jdbcTemplate.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS wompi_transaction_id VARCHAR(80)");
            jdbcTemplate.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_environment VARCHAR(10)");
            jdbcTemplate.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS service_fee DOUBLE PRECISION");
            jdbcTemplate.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP WITH TIME ZONE");
            jdbcTemplate.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_notified_at TIMESTAMP WITH TIME ZONE");
            jdbcTemplate.execute("UPDATE orders SET service_fee = 0 WHERE service_fee IS NULL");
            jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_orders_payment_reference ON orders (payment_reference)");
            log.info("Columnas de pago verificadas");
        } catch (Exception exception) {
            log.warn("No se pudieron preparar las columnas de pago: {}", exception.getMessage());
        }
    }
}
