package com.pardorps.pardrops.dto;

import java.time.Instant;

/**
 * Representación de una imagen subida a la plataforma.
 *
 * @param id           identificador del recurso.
 * @param url          URL publica con la que se sirve la imagen.
 * @param path         ruta relativa del recurso.
 * @param originalName nombre original del fichero.
 * @param mimeType     tipo MIME de la imagen.
 * @param sizeBytes    tamaño del fichero en bytes.
 * @param createdAt    fecha de subida.
 */
public record MediaAssetResponse(
        Long id,
        String url,
        String path,
        String originalName,
        String mimeType,
        long sizeBytes,
        Instant createdAt
) {
}
