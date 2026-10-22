package com.pardorps.pardrops.storage;

/**
 * Abstracción del almacén de ficheros de la plataforma.
 *
 * <p>Permite guardar y borrar las imágenes subidas sin acoplar el resto del
 * código a un sistema de archivos concreto: hoy la implementación es local
 * ({@link FileSystemMediaStorage}), pero en producción puede sustituirse por
 * un almacén de objetos (S3, Azure Blob...) sin tocar el servicio de
 * multimedia.</p>
 */
public interface MediaStorage {

    /**
     * Guarda un fichero en el almacén.
     *
     * @param storedName nombre con el que se guarda (aleatorio y único).
     * @param content    contenido binario del fichero.
     * @return datos del fichero guardado.
     */
    StoredFile store(String storedName, byte[] content);

    /**
     * Elimina un fichero del almacén.
     *
     * @param storedName nombre con el que se guardó el fichero.
     * @return {@code true} si el fichero existía y se ha eliminado.
     */
    boolean delete(String storedName);

    /**
     * Ruta pública con la que se sirve un fichero guardado.
     *
     * @param storedName nombre con el que se guardó el fichero.
     * @return ruta pública relativa (por ejemplo {@code /media/abc123.png}).
     */
    String publicPath(String storedName);

    /**
     * Ruta pública base del almacen (por ejemplo {@code /media}).
     *
     * @return la ruta base, sin barra final.
     */
    String basePublicPath();

    /**
     * Ubicacion desde la que se sirven los ficheros guardados.
     *
     * @return una URI (por ejemplo {@code file:///var/data/media/}).
     */
    String storageLocation();

    /**
     * Fichero guardado en el almacén.
     *
     * @param storedName nombre con el que se guardó.
     * @param publicPath ruta pública del fichero.
     * @param sizeBytes  tamaño del fichero en bytes.
     */
    record StoredFile(String storedName, String publicPath, long sizeBytes) {
    }
}
