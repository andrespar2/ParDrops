package com.pardorps.pardrops.config;

import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.service.CategoryService;
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
 * Ajusta el esquema de las bases de datos creadas antes de la evolución a
 * marketplace multivendedor.
 *
 * <p>Hibernate crea las tablas nuevas (tiendas, solicitudes de vendedor,
 * auditoría, etc.) sin problema, pero hay tres detalles que no puede resolver
 * sobre una base de datos ya poblada:</p>
 *
 * <ol>
 *   <li>El enum {@code users.role} se creó en H2 con los valores
 *       {@code CUSTOMER} y {@code ADMIN}; hace falta añadir {@code SELLER}
 *       conservando las cuentas existentes. Igual que
 *       {@link OrderStatusSchemaMigration}, se vuelve a declarar la columna
 *       con la lista completa de valores.</li>
 *   <li>Las columnas nuevas {@code users.status} y {@code products.active} se
 *       crean sin valor por defecto, por lo que las filas anteriores quedan
 *       con {@code NULL}. Aquí se rellenan con el valor equivalente a "activo"
 *       para que el catálogo histórico no desaparezca del marketplace.</li>
 * </ol>
 *
 * <p>La clase es idempotente: se puede ejecutar tantas veces como sea
 * necesario sin producir efectos Adverse.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class MarketplaceSchemaMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceSchemaMigration.class);

    /** Consulta el tipo de dato actual de la columna del rol. */
    private static final String ROLE_COLUMN_TYPE_QUERY = """
            SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_NAME = 'USERS' AND COLUMN_NAME = 'ROLE'
            """;

    /** Consulta si existe una columna en la tabla indicada. */
    private static final String COLUMN_EXISTS_QUERY = """
            SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_NAME = ? AND COLUMN_NAME = ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final CategoryService categoryService;

    /**
     * @param jdbcTemplate    acceso directo a la base de datos para ajustar el esquema.
     * @param categoryService servicio de categorías (importación inicial).
     */
    public MarketplaceSchemaMigration(JdbcTemplate jdbcTemplate, CategoryService categoryService) {
        this.jdbcTemplate = jdbcTemplate;
        this.categoryService = categoryService;
    }

    /**
     * Aplica los ajustes de esquema necesarios.
     *
     * @param args argumentos de línea de comandos (no se usan).
     */
    @Override
    public void run(String... args) {
        syncRoleEnum();
        backfillUserStatus();
        backfillProductActive();
        importCategories();
    }

    /**
     * Importa como categorías los nombres que ya usaban los productos.
     *
     * <p>Cuando el catálogo pierde las categorías de texto libre, esta
     * importación garantiza que ningún producto quede apuntando a una categoría
     * inexistente.</p>
     */
    private void importCategories() {
        int imported = categoryService.importExistingCategories();
        if (imported > 0) {
            log.info("Categorías importadas desde el catálogo de productos: {}", imported);
        }
    }

    /**
     * Vuelve a declarar {@code users.role} con la lista completa de roles para
     * que H2 acepte el nuevo rol {@link Role#SELLER}.
     */
    private void syncRoleEnum() {
        String currentType = jdbcTemplate.query(
                ROLE_COLUMN_TYPE_QUERY,
                resultSet -> resultSet.next() ? resultSet.getString(1) : null
        );

        if (!"ENUM".equals(currentType)) {
            return;
        }

        String values = Arrays.stream(Role.values())
                .map(Enum::name)
                .map(name -> "'" + name + "'")
                .collect(Collectors.joining(", "));

        jdbcTemplate.execute(
                "ALTER TABLE IF EXISTS users ALTER COLUMN role ENUM(" + values + ")");

        log.info("Valores de users.role sincronizados: {}", values);
    }

    /**
     * Marca como activas las cuentas anteriores a la evolución (columna con
     * valor {@code NULL}).
     */
    private void backfillUserStatus() {
        if (!columnExists("USERS", "STATUS")) {
            return;
        }
        int updated = jdbcTemplate.update(
                "UPDATE users SET status = 'ACTIVE' WHERE status IS NULL");
        if (updated > 0) {
            log.info("Cuentas existentes marcadas como activas: {}", updated);
        }
    }

    /**
     * Publica los productos anteriores a la evolución (columna con valor
     * {@code NULL}) para que sigan visibles en el catálogo.
     */
    private void backfillProductActive() {
        if (!columnExists("PRODUCTS", "ACTIVE")) {
            return;
        }
        int updated = jdbcTemplate.update(
                "UPDATE products SET active = TRUE WHERE active IS NULL");
        if (updated > 0) {
            log.info("Productos existentes marcados como activos: {}", updated);
        }
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
