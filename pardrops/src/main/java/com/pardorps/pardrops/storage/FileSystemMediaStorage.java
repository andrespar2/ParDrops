package com.pardorps.pardrops.storage;

import com.pardorps.pardrops.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Almacén de ficheros sobre el sistema de archivos local.
 *
 * <p>Guarda las imágenes en un directorio configurable (por defecto
 * {@code ./data/media}), fuera de la carpeta temporal del servidor, de modo
 * que el contenido sobreviva a un reinicio. Los ficheros se sirven despues
 * como recursos estáticos desde la ruta pública configurada.</p>
 */
@Component
public class FileSystemMediaStorage implements MediaStorage {

    /** Directorio del sistema de archivos donde se guardan los ficheros. */
    private final Path rootDirectory;

    /** Ruta pública con la que se sirven los ficheros. */
    private final String publicPath;

    /**
     * @param rootDirectory directorio donde se guardan los ficheros.
     * @param publicPath    ruta pública con la que se sirven.
     */
    public FileSystemMediaStorage(
            @Value("${app.media.storage-dir:./data/media}") String rootDirectory,
            @Value("${app.media.public-path:/media}") String publicPath
    ) {
        this.rootDirectory = Paths.get(rootDirectory).toAbsolutePath().normalize();
        this.publicPath = publicPath.endsWith("/")
                ? publicPath.substring(0, publicPath.length() - 1)
                : publicPath;
    }

    /**
     * @return directorio absoluto donde se guardan los ficheros.
     */
    public Path getRootDirectory() {
        return rootDirectory;
    }

    @Override
    public StoredFile store(String storedName, byte[] content) {
        try {
            Files.createDirectories(rootDirectory);
            Path target = resolve(storedName);
            Files.copy(new java.io.ByteArrayInputStream(content), target,
                    StandardCopyOption.REPLACE_EXISTING);
            return new StoredFile(storedName, publicPath(storedName), content.length);
        } catch (IOException exception) {
            throw ApiException.badRequest("No se pudo guardar la imagen en el servidor");
        }
    }

    @Override
    public boolean delete(String storedName) {
        try {
            return Files.deleteIfExists(resolve(storedName));
        } catch (IOException exception) {
            return false;
        }
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
        return rootDirectory.toUri().toString();
    }

    /**
     * Construye la ruta del fichero dentro del directorio del almacén.
     *
     * <p>Se comprueba que el nombre no se salga del directorio, de modo que un
     * nombre manipulado no pueda leer ni borrar ficheros ajenos.</p>
     *
     * @param storedName nombre del fichero guardado.
     * @return ruta absoluta del fichero.
     */
    private Path resolve(String storedName) {
        Path target = rootDirectory.resolve(storedName).normalize();
        if (!target.startsWith(rootDirectory)) {
            throw ApiException.badRequest("El nombre del fichero no es valido");
        }
        return target;
    }
}
