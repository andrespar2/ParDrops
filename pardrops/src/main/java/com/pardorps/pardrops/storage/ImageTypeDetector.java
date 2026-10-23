package com.pardorps.pardrops.storage;

import java.nio.charset.StandardCharsets;

/**
 * Detector del tipo de imagen a partir de los bytes del fichero.
 *
 * <p>No se fia del nombre ni del tipo MIME que declara el navegador: comprueba
 * la firma binaria del contenido. Asi un fichero que no sea una imagen (o que
 * se renombre con una extension valida) se rechaza en el servidor.</p>
 */
public final class ImageTypeDetector {

    /** Tipo MIME de las imagenes JPEG. */
    public static final String JPEG = "image/jpeg";

    /** Tipo MIME de las imagenes PNG. */
    public static final String PNG = "image/png";

    /** Tipo MIME de las imagenes GIF. */
    public static final String GIF = "image/gif";

    /** Tipo MIME de las imagenes WEBP. */
    public static final String WEBP = "image/webp";

    /** Clase utilitaria: no se instancia. */
    private ImageTypeDetector() {
    }

    /**
     * Detecta el tipo de imagen del contenido indicado.
     *
     * @param content bytes del fichero.
     * @return el tipo MIME detectado o {@code null} si no es una imagen valida.
     */
    public static String detect(byte[] content) {
        if (content == null || content.length < 12) {
            return null;
        }

        if ((content[0] & 0xFF) == 0xFF && (content[1] & 0xFF) == 0xD8
                && (content[2] & 0xFF) == 0xFF) {
            return JPEG;
        }

        if ((content[0] & 0xFF) == 0x89 && content[1] == 'P' && content[2] == 'N'
                && content[3] == 'G' && (content[4] & 0xFF) == 0x0D
                && (content[5] & 0xFF) == 0x0A && (content[6] & 0xFF) == 0x1A
                && (content[7] & 0xFF) == 0x0A) {
            return PNG;
        }

        String header = new String(content, 0, 6, StandardCharsets.US_ASCII);
        if ("GIF87a".equals(header) || "GIF89a".equals(header)) {
            return GIF;
        }

        String riff = new String(content, 0, 4, StandardCharsets.US_ASCII);
        String webp = new String(content, 8, 4, StandardCharsets.US_ASCII);
        if ("RIFF".equals(riff) && "WEBP".equals(webp)) {
            return WEBP;
        }

        return null;
    }

    /**
     * Devuelve la extension del fichero para el tipo MIME indicado.
     *
     * @param mimeType tipo MIME de la imagen.
     * @return extension sin punto (por ejemplo {@code png}).
     */
    public static String extensionFor(String mimeType) {
        if (JPEG.equals(mimeType)) {
            return "jpg";
        }
        if (PNG.equals(mimeType)) {
            return "png";
        }
        if (GIF.equals(mimeType)) {
            return "gif";
        }
        if (WEBP.equals(mimeType)) {
            return "webp";
        }
        return "bin";
    }
}
