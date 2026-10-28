package com.pardorps.pardrops.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pruebas de la migración de privacidad de tiendas.
 */
@SpringBootTest
class StorePrivacySchemaMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StorePrivacySchemaMigration migration;

    @Test
    @DisplayName("Las columnas públicas y privadas de la tienda existen")
    void privacyColumnsExist() {
        Integer columns = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'STORES'
                  AND COLUMN_NAME IN ('PUBLIC_EMAIL', 'PUBLIC_PHONE', 'CITY', 'NOTIFICATION_EMAIL')
                """, Integer.class);

        assertEquals(4, columns);
    }

    @Test
    @DisplayName("La migración se puede ejecutar varias veces")
    void isIdempotent() {
        migration.run();
        migration.run();
    }
}
