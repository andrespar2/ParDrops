package com.pardorps.pardrops.config;

import com.pardorps.pardrops.model.OrderStatus;
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
 * Sincroniza el tipo de la columna {@code orders.status} con {@link OrderStatus}.
 *
 * <p>Hibernate crea esa columna como un {@code ENUM} nativo de H2 con la lista
 * de estados vigente en el momento de crear la tabla. Como
 * {@code spring.jpa.hibernate.ddl-auto=update} no modifica los valores de un
 * {@code ENUM} ya existente, una base de datos creada antes de añadir un estado
 * nuevo (por ejemplo {@code PREPARED}) rechazaría ese valor.</p>
 *
 * <p>Esta clase vuelve a declarar la columna con la lista actual de estados,
 * conservando los pedidos ya guardados. Es idempotente (H2 admite repetir la
 * misma declaración) y no hace nada si la columna no existe todavía —el caso de
 * una base de datos nueva, que Hibernate ya crea con los estados actuales— ni
 * si la base de datos no usa el tipo {@code ENUM}.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OrderStatusSchemaMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusSchemaMigration.class);

    /** Consulta el tipo de dato actual de la columna del estado. */
    private static final String COLUMN_TYPE_QUERY = """
            SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_NAME = 'ORDERS' AND COLUMN_NAME = 'STATUS'
            """;

    private final JdbcTemplate jdbcTemplate;

    /**
     * @param jdbcTemplate acceso directo a la base de datos para ajustar el esquema.
     */
    public OrderStatusSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Ajusta el {@code ENUM} del estado si es necesario.
     *
     * @param args argumentos de línea de comandos (no se usan).
     */
    @Override
    public void run(String... args) {
        String currentType = jdbcTemplate.query(
                COLUMN_TYPE_QUERY,
                resultSet -> resultSet.next() ? resultSet.getString(1) : null
        );

        if (!"ENUM".equals(currentType)) {
            return;
        }

        String values = Arrays.stream(OrderStatus.values())
                .map(Enum::name)
                .map(name -> "'" + name + "'")
                .collect(Collectors.joining(", "));

        jdbcTemplate.execute(
                "ALTER TABLE IF EXISTS orders ALTER COLUMN status ENUM(" + values + ")");

        log.info("Valores de orders.status sincronizados: {}", values);
    }
}
