package com.pardorps.pardrops.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapta la cadena de conexión que entregan las plataformas de despliegue al
 * formato que entiende HikariCP.
 *
 * <p>Neon, Render y Heroku publican la base de datos como {@code DATABASE_URL}
 * con el esquema {@code postgresql://usuario:clave@host/base?params}, mientras
 * que Spring Boot espera {@code jdbc:postgresql://host/base?params}. Esta clase
 * convierte una en otra y deja el resultado como propiedad del sistema, que
 * tiene prioridad sobre {@code application.properties}.</p>
 *
 * <p>Si la variable no existe (desarrollo local con H2) no se hace nada.</p>
 */
public final class DatabaseUrlSupport {

    private static final Logger log = LoggerFactory.getLogger(DatabaseUrlSupport.class);

    /** Nombre de la propiedad que consume Spring Boot. */
    static final String SPRING_DATASOURCE_URL = "spring.datasource.url";

    private DatabaseUrlSupport() {
    }

    /**
     * Si el entorno define {@code DATABASE_URL} y no hay una URL explícita,
     * registra la versión JDBC como propiedad del sistema.
     */
    public static void applyEnvironmentDatabaseUrl() {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }
        if (System.getenv("SPRING_DATASOURCE_URL") != null
                || System.getProperty(SPRING_DATASOURCE_URL) != null) {
            return;
        }
        String jdbcUrl = toJdbcUrl(databaseUrl);
        System.setProperty(SPRING_DATASOURCE_URL, jdbcUrl);
        log.info("DATABASE_URL convertida a URL JDBC para PostgreSQL: {}", safeUrl(jdbcUrl));
    }

    /**
     * Convierte una URL {@code postgres://} o {@code postgresql://} en su
     * equivalente JDBC.
     *
     * <p>Se añade {@code prepareThreshold=0} cuando la URL apunta al pooler de
     * Neon, porque el modo de transacción de PgBouncer no admite sentencias
     * preparadas persistentes.</p>
     *
     * @param url cadena de conexión de la plataforma.
     * @return la URL en formato JDBC.
     */
    public static String toJdbcUrl(String url) {
        String trimmed = url.trim();
        String jdbc = trimmed;
        if (jdbc.startsWith("postgresql://")) {
            jdbc = "jdbc:postgresql://" + jdbc.substring("postgresql://".length());
        } else if (jdbc.startsWith("postgres://")) {
            jdbc = "jdbc:postgresql://" + jdbc.substring("postgres://".length());
        }

        if (jdbc.contains("-pooler") && !jdbc.contains("prepareThreshold=")) {
            jdbc = jdbc + (jdbc.contains("?") ? "&" : "?") + "prepareThreshold=0";
        }
        return jdbc;
    }

    /**
     * Oculta la contraseña antes de escribir la URL en el registro.
     *
     * @param url URL JDBC completa.
     * @return la URL con la contraseña sustituida por asteriscos.
     */
    static String safeUrl(String url) {
        return url.replaceAll("//([^:/@]+):[^@]+@", "//$1:***@");
    }
}
