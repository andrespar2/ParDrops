package com.pardorps.pardrops.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de la migración que completa el esquema del acceso con Google.
 */
@SpringBootTest
class GoogleSchemaMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private GoogleSchemaMigration migration;

    @Test
    @DisplayName("Las columnas del proveedor existen en la tabla de cuentas")
    void providerColumnsExist() {
        assertEquals(3, countColumns());
    }

    @Test
    @DisplayName("Las cuentas anteriores se marcan como locales")
    void backfillsLegacyAccountsAsLocal() {
        jdbcTemplate.update("""
                INSERT INTO users (email, password, full_name, role, status, auth_provider, created_at)
                VALUES ('legacy@pardrops.com', 'hash', 'Cuenta anterior', 'CUSTOMER', 'ACTIVE', NULL, CURRENT_TIMESTAMP)
                """);

        migration.run();

        String provider = jdbcTemplate.queryForObject(
                "SELECT auth_provider FROM users WHERE email = 'legacy@pardrops.com'",
                String.class);
        assertEquals("LOCAL", provider);
    }

    @Test
    @DisplayName("La auditoría admite las acciones del acceso con Google")
    void auditEnumAcceptsGoogleActions() {
        jdbcTemplate.update("DELETE FROM audit_logs");
        jdbcTemplate.execute(
                "ALTER TABLE audit_logs ALTER COLUMN action ENUM('USER_SUSPENDED')");

        migration.run();

        jdbcTemplate.update("""
                INSERT INTO audit_logs (action, entity_type, entity_id, details, created_at)
                VALUES ('GOOGLE_ACCOUNT_CREATED', 'USER', '1', 'Prueba', CURRENT_TIMESTAMP)
                """);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE action = 'GOOGLE_ACCOUNT_CREATED'",
                Integer.class);
        assertTrue(count != null && count == 1);
    }

    @Test
    @DisplayName("La migración se puede ejecutar varias veces sin errores")
    void isIdempotent() {
        migration.run();
        migration.run();

        Integer indexes = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES
                WHERE TABLE_NAME = 'USERS' AND INDEX_NAME = 'UK_USERS_PROVIDER'
                """, Integer.class);
        assertTrue(indexes != null && indexes > 0);
    }

    private int countColumns() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'USERS'
                  AND COLUMN_NAME IN ('AUTH_PROVIDER', 'PROVIDER_ID', 'AVATAR_URL')
                """, Integer.class);
        return count == null ? 0 : count;
    }
}
