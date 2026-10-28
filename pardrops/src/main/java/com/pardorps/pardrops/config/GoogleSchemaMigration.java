package com.pardorps.pardrops.config;

import com.pardorps.pardrops.model.AuditAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Completa el esquema de las bases de datos creadas antes del acceso con
 * Google.
 *
 * <p>Hibernate añade las columnas nuevas ({@code auth_provider},
 * {@code provider_id} y {@code avatar_url}) al arrancar, pero las cuentas
 * existentes quedan con {@code auth_provider} nulo. Esta migración las marca
 * como {@link com.pardorps.pardrops.model.AuthProvider#LOCAL}, garantiza el
 * índice único que relaciona proveedor e identificador externo y sincroniza el
 * {@code ENUM} de auditoría con las acciones nuevas.</p>
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

    private static final String AUDIT_ACTION_TYPE_QUERY = """
            SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_NAME = 'AUDIT_LOGS' AND COLUMN_NAME = 'ACTION'
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
        if (columnExists("USERS", "AUTH_PROVIDER")) {
            backfillProvider();
            ensureProviderIndex();
        }
        syncAuditActionEnum();
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

    /**
     * Sincroniza el {@code ENUM} nativo de {@code audit_logs.action} con las
     * acciones actuales.
     *
     * <p>Hibernate añade los valores nuevos al enum de Java, pero no modifica
     * un {@code ENUM} de H2 ya creado. Sin esta sincronización, registrar una
     * cuenta creada con Google falla en las bases de datos existentes.</p>
     */
    private void syncAuditActionEnum() {
        String currentType = jdbcTemplate.query(
                AUDIT_ACTION_TYPE_QUERY,
                resultSet -> resultSet.next() ? resultSet.getString(1) : null
        );

        if (!"ENUM".equals(currentType)) {
            return;
        }

        String values = Arrays.stream(AuditAction.values())
                .map(Enum::name)
                .map(name -> "'" + name + "'")
                .collect(Collectors.joining(", "));

        jdbcTemplate.execute(
                "ALTER TABLE IF EXISTS audit_logs ALTER COLUMN action ENUM(" + values + ")");

        log.info("Valores de audit_logs.action sincronizados: {}", values);
    }
}
