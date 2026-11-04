package com.pardorps.pardrops.config;

import com.pardorps.pardrops.model.AuditAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Sincroniza la restricción de {@code audit_logs.action} con el enum Java.
 *
 * <p>Hibernate crea en PostgreSQL una restricción {@code CHECK} con los valores
 * del enum. Al añadir acciones nuevas (por ejemplo, las de administradores) la
 * restricción antigua rechaza las inserciones. Esta migración la reemplaza por
 * la lista actual de valores y es idempotente.</p>
 */
@Component
public class AuditActionConstraintMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AuditActionConstraintMigration.class);

    /** Nombre de la restricción generada por Hibernate. */
    private static final String CONSTRAINT = "audit_logs_action_check";

    private final JdbcTemplate jdbcTemplate;

    /**
     * @param jdbcTemplate acceso directo a la base de datos.
     */
    public AuditActionConstraintMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        String product;
        try {
            product = jdbcTemplate.execute((org.springframework.jdbc.core.ConnectionCallback<String>) connection ->
                    connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT));
        } catch (Exception exception) {
            log.debug("No se pudo detectar el motor de base de datos: {}", exception.getMessage());
            return;
        }

        if (!product.contains("postgres")) {
            return;
        }

        String values = Arrays.stream(AuditAction.values())
                .map(Enum::name)
                .map(name -> "'" + name + "'")
                .collect(Collectors.joining(", "));

        try {
            jdbcTemplate.execute("ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS " + CONSTRAINT);
            jdbcTemplate.execute("ALTER TABLE audit_logs ADD CONSTRAINT " + CONSTRAINT
                    + " CHECK (action IN (" + values + "))");
            log.info("Restricción de auditoría sincronizada con {} acciones", AuditAction.values().length);
        } catch (Exception exception) {
            log.warn("No se pudo sincronizar la restricción de auditoría: {}", exception.getMessage());
        }
    }
}
