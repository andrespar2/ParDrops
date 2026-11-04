package com.pardorps.pardrops.config;

import com.pardorps.pardrops.model.EmailTemplateCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Sincroniza la restricción de {@code email_templates.code} con el enum Java.
 *
 * <p>Hibernate crea en PostgreSQL una restricción {@code CHECK} con los códigos
 * existentes al crear la tabla. Al añadir plantillas nuevas esa restricción
 * rechaza las inserciones y el arranque falla. Esta migración la reemplaza por
 * la lista actual de códigos y es idempotente.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class EmailTemplateCodeConstraintMigration implements CommandLineRunner {

    private static final Logger log =
            LoggerFactory.getLogger(EmailTemplateCodeConstraintMigration.class);

    /** Nombre de la restricción generada por Hibernate. */
    private static final String CONSTRAINT = "email_templates_code_check";

    private final JdbcTemplate jdbcTemplate;

    /**
     * @param jdbcTemplate acceso directo a la base de datos.
     */
    public EmailTemplateCodeConstraintMigration(JdbcTemplate jdbcTemplate) {
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

        String values = Arrays.stream(EmailTemplateCode.values())
                .map(Enum::name)
                .map(name -> "'" + name + "'")
                .collect(Collectors.joining(", "));

        try {
            jdbcTemplate.execute("ALTER TABLE email_templates DROP CONSTRAINT IF EXISTS "
                    + CONSTRAINT);
            jdbcTemplate.execute("ALTER TABLE email_templates ADD CONSTRAINT " + CONSTRAINT
                    + " CHECK (code IN (" + values + "))");
            log.info("Restricción de plantillas sincronizada con {} códigos",
                    EmailTemplateCode.values().length);
        } catch (Exception exception) {
            log.warn("No se pudo sincronizar la restricción de plantillas: {}",
                    exception.getMessage());
        }
    }
}
