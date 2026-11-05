package com.pardorps.pardrops.config;

import com.pardorps.pardrops.model.MediaAsset;
import com.pardorps.pardrops.model.MediaFile;
import com.pardorps.pardrops.repository.MediaAssetRepository;
import com.pardorps.pardrops.repository.MediaFileRepository;
import com.pardorps.pardrops.storage.MediaStorage;
import com.pardorps.pardrops.storage.R2MediaStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Copia las imágenes históricas de PostgreSQL al almacenamiento externo.
 *
 * <p>Solo se ejecuta cuando {@code app.media.storage=r2} y
 * {@code app.media.migrate-database-to-r2=true}. Es idempotente: las imágenes
 * que ya tienen una URL externa se omiten. <strong>Nunca elimina</strong> los
 * BLOB originales ni las filas de {@code media_files}; solo actualiza las
 * referencias públicas cuando la subida se ha confirmado.</p>
 */
@Component
@Order(30)
public class MediaDatabaseToR2Migration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MediaDatabaseToR2Migration.class);

    private final MediaStorage mediaStorage;
    private final MediaFileRepository mediaFileRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final JdbcTemplate jdbcTemplate;
    private final boolean enabled;

    public MediaDatabaseToR2Migration(
            MediaStorage mediaStorage,
            MediaFileRepository mediaFileRepository,
            MediaAssetRepository mediaAssetRepository,
            JdbcTemplate jdbcTemplate,
            @Value("${app.media.migrate-database-to-r2:false}") boolean enabled
    ) {
        this.mediaStorage = mediaStorage;
        this.mediaFileRepository = mediaFileRepository;
        this.mediaAssetRepository = mediaAssetRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled || !(mediaStorage instanceof R2MediaStorage)) {
            return;
        }

        Map<String, MediaAsset> assets = new HashMap<>();
        for (MediaAsset asset : mediaAssetRepository.findAll()) {
            assets.put(asset.getStoredName(), asset);
        }

        List<MediaFile> files = mediaFileRepository.findAll();
        int migrated = 0;
        int skipped = 0;
        int failed = 0;

        for (MediaFile file : files) {
            MediaAsset asset = assets.get(file.getStoredName());
            if (asset == null) {
                skipped++;
                continue;
            }
            if (asset.getPublicPath() != null
                    && (asset.getPublicPath().startsWith("http://")
                    || asset.getPublicPath().startsWith("https://"))) {
                skipped++;
                continue;
            }
            try {
                MediaStorage.StoredFile stored = mediaStorage.store(
                        file.getStoredName(), file.getContent());
                String oldSuffix = "%/media/" + file.getStoredName();
                String newUrl = stored.publicPath();

                jdbcTemplate.update(
                        "update product_images set url = ? where url like ?",
                        newUrl, oldSuffix);
                jdbcTemplate.update(
                        "update product_variants set variant_image_url = ? "
                                + "where variant_image_url like ?",
                        newUrl, oldSuffix);
                jdbcTemplate.update(
                        "update stores set logo_url = ? where logo_url like ?",
                        newUrl, oldSuffix);
                jdbcTemplate.update(
                        "update stores set banner_url = ? where banner_url like ?",
                        newUrl, oldSuffix);
                jdbcTemplate.update(
                        "update product_review_images set image_url = ? where image_url like ?",
                        newUrl, oldSuffix);
                jdbcTemplate.update(
                        "update media_assets set public_path = ? where stored_name = ?",
                        newUrl, file.getStoredName());

                migrated++;
            } catch (RuntimeException exception) {
                failed++;
                log.warn("No se pudo migrar la imagen {}: {}",
                        file.getStoredName(), exception.getMessage());
            }
        }

        log.info("Migración de imágenes a R2: {} migradas, {} omitidas, {} fallidas. "
                        + "Los originales de media_files se conservan.",
                migrated, skipped, failed);
    }
}
