package com.pardorps.pardrops.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Crea los índices que sostienen las consultas reales del marketplace.
 *
 * <p>Hibernate sigue gestionando el esquema, pero no crea índices para las
 * columnas de filtro y orden. Esta migración es idempotente: puede ejecutarse
 * en cada arranque tanto en H2 como en PostgreSQL. No elimina ni renombra
 * columnas y no toca los índices únicos que ya generan las restricciones.</p>
 */
@Component
@Order(20)
public class DatabaseIndexMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseIndexMigration.class);

    /** Índices justificados por filtros, uniones y ordenaciones reales. */
    private static final List<String> INDEXES = List.of(
            // Catálogo público y paneles filtran por publicación y vendedor.
            "CREATE INDEX IF NOT EXISTS idx_products_active_seller_created "
                    + "ON products (active, seller_id, created_at)",
            "CREATE INDEX IF NOT EXISTS idx_products_seller_active "
                    + "ON products (seller_id, active)",
            "CREATE INDEX IF NOT EXISTS idx_products_active_created "
                    + "ON products (active, created_at)",
            "CREATE INDEX IF NOT EXISTS idx_products_active_price "
                    + "ON products (active, price)",
            // Ranking: calidad y popularidad.
            "CREATE INDEX IF NOT EXISTS idx_products_active_rating "
                    + "ON products (active, rating, review_count)",
            // Colecciones de producto: se cargan por product_id y se filtran
            // por categoría en la búsqueda del panel.
            "CREATE INDEX IF NOT EXISTS idx_product_images_product "
                    + "ON product_images (product_id)",
            "CREATE INDEX IF NOT EXISTS idx_product_categories_product "
                    + "ON product_categories (product_id)",
            "CREATE INDEX IF NOT EXISTS idx_product_categories_category "
                    + "ON product_categories (category, product_id)",
            "CREATE INDEX IF NOT EXISTS idx_product_tags_tag "
                    + "ON product_tags (tag)",
            "CREATE INDEX IF NOT EXISTS idx_product_variants_product "
                    + "ON product_variants (product_id)",
            // Reseñas: listado por producto y moderación.
            "CREATE INDEX IF NOT EXISTS idx_reviews_product_created "
                    + "ON product_reviews (product_id, created_at)",
            "CREATE INDEX IF NOT EXISTS idx_reviews_user "
                    + "ON product_reviews (user_id)",
            // Pedidos: historial del cliente y tablero de administración.
            "CREATE INDEX IF NOT EXISTS idx_orders_user_created "
                    + "ON orders (user_id, created_at)",
            "CREATE INDEX IF NOT EXISTS idx_orders_status_created "
                    + "ON orders (status, created_at)",
            "CREATE INDEX IF NOT EXISTS idx_order_items_order "
                    + "ON order_items (order_id)",
            "CREATE INDEX IF NOT EXISTS idx_order_items_product "
                    + "ON order_items (product_id)",
            "CREATE INDEX IF NOT EXISTS idx_order_items_sub_order "
                    + "ON order_items (sub_order_id)",
            "CREATE INDEX IF NOT EXISTS idx_sub_orders_seller_created "
                    + "ON order_sub_orders (seller_id, created_at)",
            "CREATE INDEX IF NOT EXISTS idx_sub_orders_order "
                    + "ON order_sub_orders (order_id)",
            // Bandeja de avisos y biblioteca de imágenes.
            "CREATE INDEX IF NOT EXISTS idx_notifications_user_read_created "
                    + "ON notifications (user_id, is_read, created_at)",
            "CREATE INDEX IF NOT EXISTS idx_media_assets_owner_created "
                    + "ON media_assets (owner_id, created_at)",
            // Solicitudes y auditoría del panel.
            "CREATE INDEX IF NOT EXISTS idx_seller_applications_status_created "
                    + "ON seller_applications (status, created_at)",
            "CREATE INDEX IF NOT EXISTS idx_audit_logs_created "
                    + "ON audit_logs (created_at)",
            "CREATE INDEX IF NOT EXISTS idx_users_role_status "
                    + "ON users (role, status)",
            "CREATE INDEX IF NOT EXISTS idx_categories_active_featured "
                    + "ON categories (active, featured)",
            "CREATE INDEX IF NOT EXISTS idx_stores_status_name "
                    + "ON stores (status, name)"
    );

    private final JdbcTemplate jdbcTemplate;

    public DatabaseIndexMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        int created = 0;
        for (String sql : INDEXES) {
            try {
                jdbcTemplate.execute(sql);
                created++;
            } catch (RuntimeException exception) {
                // Un índice ausente no debe impedir el arranque; se registra
                // para poder corregir el nombre de una tabla en una migración.
                log.warn("No se pudo crear un índice: {}. Causa: {}",
                        sql, exception.getMessage());
            }
        }
        log.info("Migración de índices completada: {} sentencias verificadas", created);
    }
}
