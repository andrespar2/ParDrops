package com.pardorps.pardrops.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Completa el esquema de las bases de datos creadas antes del acceso con
 * Google.
 *
 * <p>Hibernate añade las columnas nuevas ({@code auth_provider},
 * {@code provider_id} y {@code avatar_url}) al arrancar, pero las cuentas
 * existentes quedan con {@code auth_provider} nulo. Esta migración las marca
 * como {@link com.pardorps.pardrops.model.AuthProvider#LOCAL} y garantiza el
 * índice único que relaciona proveedor e identificador externo.</p>
 *
 * <p>Es idempotente: se puede ejecutar en cada arranque sin destruir datos.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 15)
public class GoogleSchemaMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(GoogleSchemaMigration.class);

    private static final String COLUMN_EXISTS_QUERY = """
            SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_NAME = ? AND COLUMN_NAME = ?
            """;

    private static final String PROVIDER_INDEX_NAME = "uk_users_provider";

    private final JdbcTemplate jdbcTemplate;

    /**
     * @param jdbcTemplate acceso directo a la base de datos para ajustar el esquema.
     */
    public GoogleSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Aplica los ajustes de esquema necesarios.
     *
     * @param args argumentos de línea de comandos (no se usan).
     */
    @Override
    public void run(String... args) {
        if (!columnExists("USERS", "AUTH_PROVIDER")) {
            return;
        }
        backfillProvider();
        ensureProviderIndex();
    }

    /**
     * Marca como locales las cuentas anteriores al acceso con Google.
     */
    private void backfillProvider() {
        int updated = jdbcTemplate.update(
                "UPDATE users SET auth_provider = 'LOCAL' WHERE auth_provider IS NULL");
        if (updated > 0) {
            log.info("Cuentas existentes marcadas como proveedor LOCAL: {}", updated);
        }
    }

    /**
     * Crea el índice único de proveedor e identificador si aún no existe.
     *
     * <p>H2 permite varios valores nulos en un índice único, de modo que las
     * cuentas locales (sin identificador externo) no colisionan entre sí.</p>
     */
    private void ensureProviderIndex() {
        jdbcTemplate.execute(
                "CREATE UNIQUE INDEX IF NOT EXISTS " + PROVIDER_INDEX_NAME
                        + " ON users(auth_provider, provider_id)");
    }

    /**
     * Comprueba si una columna existe en la base de datos.
     *
     * @param table  nombre de la tabla (en mayúsculas).
     * @param column nombre de la columna (en mayúsculas).
     * @return {@code true} si la columna existe.
     */
    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject(
                COLUMN_EXISTS_QUERY, Integer.class, table, column);
        return count != null && count > 0;
    }
}
