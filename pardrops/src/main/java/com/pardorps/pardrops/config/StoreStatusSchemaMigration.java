package com.pardorps.pardrops.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Amplía la restricción de estado de las tiendas para admitir DELETED.
 *
 * <p>La base de datos de producción se creó con una comprobación
 * {@code stores_status_check} que solo conocía los estados históricos. La
 * eliminación lógica necesita persistir {@code DELETED}, así que la restricción
 * se reemplaza de forma idempotente. En H2 no se aplica porque no tiene la
 * misma restricción.</p>
 */
@Component
@Order(15)
public class StoreStatusSchemaMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StoreStatusSchemaMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public StoreStatusSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        String product;
        try {
            product = jdbcTemplate.execute(
                    (org.springframework.jdbc.core.ConnectionCallback<String>) connection ->
                            connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT));
        } catch (Exception exception) {
            log.debug("No se pudo detectar el motor de base de datos: {}", exception.getMessage());
            return;
        }

        if (!product.contains("postgres")) {
            return;
        }

        try {
            jdbcTemplate.execute(
                    "ALTER TABLE stores DROP CONSTRAINT IF EXISTS stores_status_check");
            jdbcTemplate.execute("""
                    ALTER TABLE stores ADD CONSTRAINT stores_status_check
                    CHECK (status IN ('ACTIVE', 'SUSPENDED', 'PENDING', 'DELETED'))
                    """);
            log.info("Restricción stores_status_check actualizada con DELETED");
        } catch (Exception exception) {
            log.warn("No se pudo actualizar stores_status_check: {}", exception.getMessage());
        }
    }
}
