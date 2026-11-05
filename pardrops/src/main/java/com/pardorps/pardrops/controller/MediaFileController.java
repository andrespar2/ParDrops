package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.MediaFile;
import com.pardorps.pardrops.repository.MediaFileRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Sirve las imágenes guardadas en PostgreSQL.
 *
 * <p>Solo se activa en el perfil {@code prod}, donde
 * {@link com.pardorps.pardrops.storage.DatabaseMediaStorage} sustituye al
 * almacén en disco. La ruta es pública: las imágenes se muestran en el catálogo
 * sin necesidad de sesión.</p>
 */
@RestController
@Profile("prod")
@RequestMapping("/media")
public class MediaFileController {

    /** Acceso al contenido de las imágenes. */
    private final MediaFileRepository mediaFileRepository;

    /**
     * @param mediaFileRepository acceso al contenido de las imágenes.
     */
    public MediaFileController(MediaFileRepository mediaFileRepository) {
        this.mediaFileRepository = mediaFileRepository;
    }

    /**
     * Devuelve el contenido binario de una imagen.
     *
     * @param storedName nombre único del fichero.
     * @return 200 con los bytes y su tipo MIME, o 404 si no existe.
     */
    @GetMapping("/{storedName:.+}")
    public ResponseEntity<byte[]> serve(@PathVariable String storedName) {
        MediaFile file = mediaFileRepository.findByStoredName(storedName)
                .orElseThrow(() -> ApiException.notFound("La imagen no existe"));

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(file.getMimeType());
        } catch (RuntimeException exception) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .eTag(storedName)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .body(file.getContent());
    }
}
