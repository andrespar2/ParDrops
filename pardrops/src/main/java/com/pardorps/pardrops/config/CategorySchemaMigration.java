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
 * Añade las columnas de jerarquía y colecciones a {@code categories}.
 *
 * <p>Hibernate no puede añadir columnas {@code NOT NULL} a una tabla que ya
 * tiene filas, así que se crean aquí con valor por defecto antes de que el
 * sembrador de categorías consulte la tabla. Es idempotente y no modifica los
 * datos existentes.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 15)
public class CategorySchemaMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CategorySchemaMigration.class);

    private final JdbcTemplate jdbcTemplate;

    /**
     * @param jdbcTemplate acceso directo a la base de datos.
     */
    public CategorySchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        String product;
        try {
            product = jdbcTemplate.getDataSource() == null
                    ? ""
                    : jdbcTemplate.getDataSource().getConnection()
                    .getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
        } catch (Exception exception) {
            log.debug("No se pudo detectar el motor de base de datos: {}", exception.getMessage());
            return;
        }

        if (!product.contains("postgres")) {
            return;
        }

        try {
            jdbcTemplate.execute("ALTER TABLE categories ADD COLUMN IF NOT EXISTS parent_id BIGINT");
            jdbcTemplate.execute("ALTER TABLE categories ADD COLUMN IF NOT EXISTS sort_order INTEGER NOT NULL DEFAULT 0");
            jdbcTemplate.execute("ALTER TABLE categories ADD COLUMN IF NOT EXISTS image_url VARCHAR(500)");
            jdbcTemplate.execute("ALTER TABLE categories ADD COLUMN IF NOT EXISTS collection BOOLEAN NOT NULL DEFAULT FALSE");
            log.info("Columnas de categorías verificadas");
        } catch (Exception exception) {
            log.warn("No se pudieron preparar las columnas de categorías: {}", exception.getMessage());
        }
    }
}
