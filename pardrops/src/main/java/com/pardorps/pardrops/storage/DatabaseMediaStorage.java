package com.pardorps.pardrops.storage;

import com.pardorps.pardrops.model.MediaFile;
import com.pardorps.pardrops.repository.MediaFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Almacén de imágenes cuyo contenido vive en PostgreSQL.
 *
 * <p>Se activa en el perfil {@code prod}. El plan gratuito de Render no ofrece
 * disco persistente, así que guardar los ficheros en el sistema de archivos del
 * contenedor los perdería en cada despliegue. Guardándolos en la base de datos
 * el catálogo conserva sus imágenes entre reinicios.</p>
 *
 * <p>Las imágenes se sirven desde {@code /media/{nombre}} mediante
 * {@link com.pardorps.pardrops.controller.MediaFileController}.</p>
 */
@Component
@Profile("prod")
public class DatabaseMediaStorage implements MediaStorage {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMediaStorage.class);

    /** Acceso al contenido almacenado. */
    private final MediaFileRepository mediaFileRepository;

    /** Ruta pública con la que se sirven los ficheros. */
    private final String publicPath;

    /**
     * @param mediaFileRepository acceso al contenido almacenado.
     * @param publicPath          ruta pública base (por defecto {@code /media}).
     */
    public DatabaseMediaStorage(
            MediaFileRepository mediaFileRepository,
            @Value("${app.media.public-path:/media}") String publicPath
    ) {
        this.mediaFileRepository = mediaFileRepository;
        this.publicPath = publicPath.endsWith("/")
                ? publicPath.substring(0, publicPath.length() - 1)
                : publicPath;
    }

    @Override
    public StoredFile store(String storedName, byte[] content) {
        String mimeType = ImageTypeDetector.detect(content);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        mediaFileRepository.save(new MediaFile(storedName, mimeType, content, Instant.now()));
        log.debug("Imagen guardada en PostgreSQL: {} ({} bytes)", storedName, content.length);
        return new StoredFile(storedName, publicPath(storedName), content.length);
    }

    @Override
    public boolean delete(String storedName) {
        if (!mediaFileRepository.existsById(storedName)) {
            return false;
        }
        mediaFileRepository.deleteById(storedName);
        return true;
    }

    @Override
    public String publicPath(String storedName) {
        return publicPath + "/" + storedName;
    }

    @Override
    public String basePublicPath() {
        return publicPath;
    }

    @Override
    public String storageLocation() {
        // El contenido no se sirve desde disco en producción; la ruta se
        // mantiene para cumplir el contrato de la interfaz.
        return "file:./data/media/";
    }
}
