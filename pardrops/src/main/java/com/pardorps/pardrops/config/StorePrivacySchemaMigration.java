package com.pardorps.pardrops.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Completa las columnas de privacidad de las tiendas.
 *
 * <p>Hibernate añade las columnas nuevas, pero las tiendas antiguas no tienen
 * correo de notificaciones. Se copia el contacto histórico a
 * {@code notification_email} para no perder los avisos de pedidos. Los campos
 * públicos se dejan vacíos a propósito: el vendedor debe decidir qué muestra.
 * </p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 26)
public class StorePrivacySchemaMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StorePrivacySchemaMigration.class);

    private final JdbcTemplate jdbcTemplate;

    /**
     * @param jdbcTemplate acceso directo a la base de datos.
     */
    public StorePrivacySchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        if (!columnExists("STORES", "NOTIFICATION_EMAIL")) {
            return;
        }
        int updated = jdbcTemplate.update("""
                UPDATE stores
                SET notification_email = contact_email
                WHERE notification_email IS NULL AND contact_email IS NOT NULL
                """);
        if (updated > 0) {
            log.info("Correos de notificación de tiendas completados: {}", updated);
        }
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = ? AND COLUMN_NAME = ?
                """, Integer.class, table, column);
        return count != null && count > 0;
    }
}
