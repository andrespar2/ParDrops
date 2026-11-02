import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Copia los datos de la base H2 de desarrollo a PostgreSQL (Neon).
 *
 * El fichero H2 NO se abre directamente: se copia antes a una carpeta de
 * respaldo (con su hash SHA-256) y se lee la copia, de modo que la base original
 * queda intacta aunque la migración falle.
 *
 * Uso:
 *   java --class-path h2.jar:postgresql.jar tools/MigrateH2ToPostgres.java
 *
 * Variables de entorno:
 *   H2_FILE                 ruta del fichero .mv.db (sin extensión)
 *   SPRING_DATASOURCE_URL   jdbc:postgresql://...
 *   SPRING_DATASOURCE_USERNAME / SPRING_DATASOURCE_PASSWORD
 *   MIGRATION_BACKUP_DIR    carpeta donde guardar la copia de seguridad
 *
 * El destino debe tener ya el esquema creado (arrancar la aplicación una vez
 * con APP_SEED_ENABLED=false). Las filas se insertan con ON CONFLICT DO NOTHING
 * y se reintentan las tablas con claves foráneas hasta que no avanza ninguna.
 */
public class MigrateH2ToPostgres {

    public static void main(String[] args) throws Exception {
        String h2File = env("H2_FILE", "./data/pardropsdb");
        String targetUrl = requireEnv("SPRING_DATASOURCE_URL");
        String user = requireEnv("SPRING_DATASOURCE_USERNAME");
        String password = requireEnv("SPRING_DATASOURCE_PASSWORD");
        String backupDir = env("MIGRATION_BACKUP_DIR", "./backups/h2-migration");

        Path source = Paths.get(h2File + ".mv.db");
        if (!Files.exists(source)) {
            throw new IllegalStateException("No existe el fichero H2: " + source.toAbsolutePath());
        }

        // 1. Copia de seguridad verificable
        Path backupFolder = Paths.get(backupDir, "h2-" + System.currentTimeMillis());
        Files.createDirectories(backupFolder);
        Path copy = backupFolder.resolve("pardropsdb.mv.db");
        Files.copy(source, copy, StandardCopyOption.REPLACE_EXISTING);
        System.out.printf("Copia de seguridad: %s (%d bytes, sha256=%s)%n",
                copy, Files.size(copy), sha256(copy));

        String h2CopyUrl = "jdbc:h2:file:" + copy.toAbsolutePath().toString()
                .replaceFirst("\\.mv\\.db$", "") + ";DB_CLOSE_ON_EXIT=FALSE";

        try (Connection h2 = DriverManager.getConnection(h2CopyUrl, "sa", "");
             Connection pg = DriverManager.getConnection(targetUrl, user, password)) {

            List<String> tables = new ArrayList<>();
            try (Statement st = h2.createStatement();
                 ResultSet rs = st.executeQuery("""
                         SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
                         WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE = 'BASE TABLE'
                         ORDER BY TABLE_NAME""")) {
                while (rs.next()) {
                    tables.add(rs.getString(1));
                }
            }
            System.out.println("Tablas en H2: " + tables.size());

            Map<String, Integer> pending = new LinkedHashMap<>();
            for (String table : tables) {
                pending.put(table, count(h2, table));
            }

            Map<String, Integer> inserted = new LinkedHashMap<>();
            int failedTotal = 0;

            // 2. Varias pasadas: las claves foráneas impiden insertar un hijo
            //    antes que su padre, así que se reintenta hasta que no avanza.
            for (int pass = 1; pass <= 12; pass++) {
                int moved = 0;
                for (String table : tables) {
                    if (!pending.containsKey(table)) {
                        continue;
                    }
                    if (!tableExists(pg, table)) {
                        System.out.printf("AVISO: la tabla %s no existe en PostgreSQL; se omite%n", table);
                        pending.remove(table);
                        continue;
                    }
                    Result result = copyTable(h2, pg, table, pass > 2);
                    inserted.merge(table, result.inserted, Integer::sum);
                    moved += result.inserted;
                    failedTotal = result.failed;

                    if (result.inserted + result.skipped >= pending.get(table)) {
                        System.out.printf("OK  %-24s filas H2=%-5d insertadas=%-5d ya existentes=%-5d%n",
                                table, pending.get(table), inserted.get(table), result.skipped);
                        pending.remove(table);
                    }
                }
                if (pending.isEmpty() || moved == 0) {
                    break;
                }
            }

            // 3. Sincronizar secuencias para que los nuevos INSERT no colisionen
            for (String table : tables) {
                fixSequence(pg, table);
            }

            // 4. Verificación: recuento origen vs destino
            System.out.println("\n=== VERIFICACIÓN ===");
            boolean ok = true;
            for (String table : tables) {
                int sourceRows = count(h2, table);
                int targetRows = tableExists(pg, table) ? count(pg, table) : 0;
                String status = targetRows >= sourceRows ? "OK" : "DIF";
                if (!"OK".equals(status)) {
                    ok = false;
                }
                System.out.printf("%-5s %-24s H2=%-5d PostgreSQL=%-5d%n",
                        status, table, sourceRows, targetRows);
            }

            if (!pending.isEmpty()) {
                System.out.println("Tablas sin completar: " + pending);
            }
            System.out.println(ok && pending.isEmpty()
                    ? "MIGRACIÓN COMPLETA: ninguna fila perdida"
                    : "MIGRACIÓN INCOMPLETA: revisar las tablas marcadas");
            if (!ok || !pending.isEmpty()) {
                System.exit(2);
            }
        }
    }

    private record Result(int inserted, int skipped, int failed) {
    }

    private static Result copyTable(Connection h2, Connection pg, String table, boolean verbose) {
        String lower = table.toLowerCase(Locale.ROOT);
        int inserted = 0;
        int skipped = 0;
        int failed = 0;

        try (Statement select = h2.createStatement();
             ResultSet rs = select.executeQuery("SELECT * FROM \"" + table + "\"")) {
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columns.add(meta.getColumnName(i).toLowerCase(Locale.ROOT));
            }
            Map<String, String> targetTypes = columnTypes(pg, lower);

            String placeholders = String.join(", ", Collections.nCopies(columnCount, "?"));
            boolean hasIdentity = targetTypes.containsValue("IDENTITY");
            String sql = "INSERT INTO \"" + lower + "\" ("
                    + String.join(", ", columns.stream().map(c -> "\"" + c + "\"").toList())
                    + ") " + (hasIdentity ? "OVERRIDING SYSTEM VALUE " : "")
                    + "VALUES (" + placeholders + ") ON CONFLICT DO NOTHING";

            try (PreparedStatement ps = pg.prepareStatement(sql)) {
                while (rs.next()) {
                    for (int i = 1; i <= columnCount; i++) {
                        Object value = rs.getObject(i);
                        String columnType = targetTypes.get(columns.get(i - 1));
                        bind(ps, i, value, columnType);
                    }
                    try {
                        int rows = ps.executeUpdate();
                        if (rows > 0) {
                            inserted++;
                        } else {
                            skipped++;
                        }
                    } catch (SQLException exception) {
                        failed++;
                        if (verbose) {
                            System.out.printf("  ! %s: %s%n", table, exception.getMessage());
                        }
                    }
                }
            }
        } catch (SQLException exception) {
            System.out.printf("  ! No se pudo leer %s: %s%n", table, exception.getMessage());
            return new Result(0, 0, 1);
        }
        return new Result(inserted, skipped, failed);
    }

    private static void bind(PreparedStatement ps, int index, Object value, String targetType)
            throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.NULL);
            return;
        }
        if (value instanceof byte[] bytes) {
            ps.setBytes(index, bytes);
            return;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            if (targetType != null && targetType.contains("with time zone")) {
                ps.setObject(index, OffsetDateTime.ofInstant(timestamp.toInstant(), ZoneOffset.UTC));
            } else {
                ps.setObject(index, timestamp.toLocalDateTime());
            }
            return;
        }
        if (value instanceof java.sql.Clob clob) {
            ps.setString(index, clob.getSubString(1, (int) clob.length()));
            return;
        }
        if (value instanceof java.sql.Blob blob) {
            ps.setBytes(index, blob.getBytes(1, (int) blob.length()));
            return;
        }
        ps.setObject(index, value);
    }

    private static Map<String, String> columnTypes(Connection pg, String table) {
        Map<String, String> types = new HashMap<>();
        try (PreparedStatement ps = pg.prepareStatement("""
                SELECT column_name, data_type, is_identity FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = ?""")) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    types.put(rs.getString(1),
                            "YES".equalsIgnoreCase(rs.getString(3)) ? "IDENTITY" : rs.getString(2));
                }
            }
        } catch (SQLException exception) {
            System.out.printf("  ! No se pudieron leer los tipos de %s: %s%n", table, exception.getMessage());
        }
        return types;
    }

    private static void fixSequence(Connection pg, String table) {
        String lower = table.toLowerCase(Locale.ROOT);
        try (PreparedStatement ps = pg.prepareStatement("SELECT pg_get_serial_sequence(?, 'id')")) {
            ps.setString(1, lower);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getString(1) != null) {
                    String sequence = rs.getString(1);
                    try (Statement st = pg.createStatement()) {
                        st.execute("SELECT setval('" + sequence + "', "
                                + "COALESCE((SELECT MAX(id) FROM \"" + lower + "\"), 1), "
                                + "(SELECT MAX(id) FROM \"" + lower + "\") IS NOT NULL)");
                    }
                    System.out.printf("Secuencia sincronizada: %s%n", sequence);
                }
            }
        } catch (SQLException exception) {
            System.out.printf("AVISO: no se pudo ajustar la secuencia de %s: %s%n",
                    lower, exception.getMessage());
        }
    }

    private static boolean tableExists(Connection pg, String table) {
        try (PreparedStatement ps = pg.prepareStatement("SELECT to_regclass(?)")) {
            ps.setString(1, "public.\"" + table.toLowerCase(Locale.ROOT) + "\"");
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getString(1) != null;
            }
        } catch (SQLException exception) {
            return false;
        }
    }

    private static int count(Connection connection, String table) {
        String name = table;
        try {
            if (connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("postgres")) {
                name = table.toLowerCase(Locale.ROOT);
            }
        } catch (SQLException ignored) {
            // Se usa el nombre tal cual.
        }
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM \"" + name + "\"")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException exception) {
            return -1;
        }
    }

    private static String sha256(Path file) throws IOException {
        try (var in = Files.newInputStream(file)) {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            in.transferTo(new java.io.OutputStream() {
                @Override
                public void write(int b) {
                    digest.update((byte) b);
                }

                @Override
                public void write(byte[] b, int off, int len) {
                    digest.update(b, off, len);
                }
            });
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno " + name);
        }
        return value;
    }
}
