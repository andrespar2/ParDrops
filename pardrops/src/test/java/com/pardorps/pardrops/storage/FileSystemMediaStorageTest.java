package com.pardorps.pardrops.storage;

import com.pardorps.pardrops.exception.ApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del almacen de ficheros sobre el sistema de archivos.
 */
class FileSystemMediaStorageTest {

    private Path directorio;
    private FileSystemMediaStorage storage;

    /**
     * Crea un directorio temporal propio para cada prueba.
     *
     * @throws IOException si no se puede crear el directorio.
     */
    @BeforeEach
    void setUp() throws IOException {
        directorio = Files.createTempDirectory("pardrops-media-test");
        storage = new FileSystemMediaStorage(directorio.toString(), "/media");
    }

    /**
     * Borra el directorio temporal al terminar.
     *
     * @throws IOException si no se puede borrar.
     */
    @AfterEach
    void tearDown() throws IOException {
        if (Files.exists(directorio)) {
            try (Stream<Path> rutas = Files.walk(directorio)) {
                rutas.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                        /* Limpieza best-effort del directorio temporal. */
                    }
                });
            }
        }
    }

    @Test
    @DisplayName("Guarda el fichero en el directorio configurado y lo expone en su ruta publica")
    void storesFileAndExposesPublicPath() throws IOException {
        byte[] contenido = "contenido de prueba".getBytes();

        MediaStorage.StoredFile guardado = storage.store("imagen.png", contenido);

        assertEquals("imagen.png", guardado.storedName());
        assertEquals("/media/imagen.png", guardado.publicPath());
        assertEquals(contenido.length, guardado.sizeBytes());
        assertTrue(Files.exists(directorio.resolve("imagen.png")));
        assertArrayEquals(contenido, Files.readAllBytes(directorio.resolve("imagen.png")));
    }

    @Test
    @DisplayName("El fichero sobrevive a una nueva instancia del almacen (persistencia)")
    void fileSurvivesNewStorageInstance() {
        storage.store("persistente.png", new byte[]{1, 2, 3});

        FileSystemMediaStorage otraInstancia =
                new FileSystemMediaStorage(directorio.toString(), "/media");

        assertTrue(Files.exists(otraInstancia.getRootDirectory().resolve("persistente.png")));
    }

    @Test
    @DisplayName("Elimina el fichero y avisa si ya no existia")
    void deletesFileAndReportsMissingFile() {
        storage.store("borrame.png", new byte[]{1, 2, 3});

        assertTrue(storage.delete("borrame.png"));
        assertFalse(storage.delete("borrame.png"));
    }

    @Test
    @DisplayName("La ruta publica base no termina en barra")
    void basePublicPathHasNoTrailingSlash() {
        FileSystemMediaStorage conBarra =
                new FileSystemMediaStorage(directorio.toString(), "/media/");

        assertEquals("/media", conBarra.basePublicPath());
        assertTrue(conBarra.storageLocation().startsWith("file:"));
    }

    @Test
    @DisplayName("Rechaza un nombre que intente salir del directorio del almacen")
    void rejectsNameEscapingStorageDirectory() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> storage.store("../fuera.png", new byte[]{1}));

        assertEquals(400, exception.getStatus().value());
    }
}
