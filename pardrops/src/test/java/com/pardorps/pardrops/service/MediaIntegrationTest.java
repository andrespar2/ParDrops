package com.pardorps.pardrops.service;

import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.MediaAsset;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.MediaAssetRepository;
import com.pardorps.pardrops.repository.UserRepository;
import com.pardorps.pardrops.storage.FileSystemMediaStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de integración de la subida de imagenes.
 *
 * <p>Usan el almacen real sobre disco y una base de datos en memoria: se
 * comprueba que el fichero queda en el directorio configurado (y no dentro de
 * la base de datos), que sobrevive a un reinicio del almacen y que el control
 * de acceso impide borrar la imagen de otro usuario.</p>
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:pardrops-media-test;DB_CLOSE_DELAY=-1",
        "app.media.storage-dir=./target/media-test",
        "app.media.public-path=/media",
        "app.media.max-size-bytes=5242880",
        "app.media.allowed-types=image/jpeg,image/png,image/webp,image/gif"
})
class MediaIntegrationTest {

    /** Cabecera de un fichero PNG valido. */
    private static final byte[] PNG_HEADER = {
            (byte) 0x89, 'P', 'N', 'G', (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A,
            0x00, 0x00, 0x00, 0x0D
    };

    @Autowired
    private MediaService mediaService;

    @Autowired
    private MediaAssetRepository mediaAssetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileSystemMediaStorage storage;

    private User propietario;
    private User otroUsuario;

    /**
     * Prepara dos cuentas con correos propios de cada prueba.
     *
     * @param testInfo datos de la prueba en curso.
     */
    @BeforeEach
    void setUp(TestInfo testInfo) {
        String sufijo = testInfo.getTestMethod().orElseThrow().getName();
        propietario = guardarUsuario("media-propietario-" + sufijo + "@media.test", Role.SELLER);
        otroUsuario = guardarUsuario("media-intruso-" + sufijo + "@media.test", Role.SELLER);
    }

    @Test
    @DisplayName("Guarda el fichero en disco y sus metadatos en la base de datos")
    void storesFileOnDiskAndMetadataInDatabase() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "producto.png", "image/png", PNG_HEADER);

        MediaAsset asset = mediaService.upload(file, propietario);

        // El binario esta en el almacen, no en la base de datos.
        Path enDisco = storage.getRootDirectory().resolve(asset.getStoredName());
        assertTrue(Files.exists(enDisco));
        assertEquals(PNG_HEADER.length, Files.size(enDisco));

        // Los metadatos quedan registrados.
        MediaAsset guardado = mediaAssetRepository.findById(asset.getId()).orElseThrow();
        assertEquals("producto.png", guardado.getOriginalName());
        assertEquals("image/png", guardado.getMimeType());
        assertEquals(propietario.getId(), guardado.getOwner().getId());
        assertTrue(guardado.getPublicPath().startsWith("/media/"));
    }

    @Test
    @DisplayName("La imagen persiste entre reinicios y se sirve por su ruta publica")
    void imageSurvivesRestart() {
        MockMultipartFile file = new MockMultipartFile("file", "persistente.png", "image/png", PNG_HEADER);
        MediaAsset asset = mediaService.upload(file, propietario);

        // Otra instancia del almacen apuntando al mismo directorio (reinicio).
        FileSystemMediaStorage trasReinicio =
                new FileSystemMediaStorage(storage.getRootDirectory().toString(), "/media");

        assertTrue(Files.exists(trasReinicio.getRootDirectory().resolve(asset.getStoredName())));
        assertEquals(storage.publicPath(asset.getStoredName()),
                trasReinicio.publicPath(asset.getStoredName()));
    }

    @Test
    @DisplayName("Un usuario no puede borrar la imagen de otro")
    void preventsDeletingAnotherUsersImage() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "privada.png", "image/png", PNG_HEADER);
        MediaAsset asset = mediaService.upload(file, propietario);
        Path enDisco = storage.getRootDirectory().resolve(asset.getStoredName());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> mediaService.delete(asset.getId(), otroUsuario));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertTrue(Files.exists(enDisco));
        assertTrue(mediaAssetRepository.findById(asset.getId()).isPresent());
    }

    @Test
    @DisplayName("El administrador si puede borrar la imagen de otro usuario")
    void allowsAdminToDeleteAnyImage() {
        MockMultipartFile file = new MockMultipartFile("file", "moderada.png", "image/png", PNG_HEADER);
        MediaAsset asset = mediaService.upload(file, propietario);
        User admin = guardarUsuario("media-admin-" + asset.getId() + "@media.test", Role.ADMIN);

        mediaService.delete(asset.getId(), admin);

        assertFalse(mediaAssetRepository.findById(asset.getId()).isPresent());
        assertFalse(Files.exists(storage.getRootDirectory().resolve(asset.getStoredName())));
    }

    @Test
    @DisplayName("Un fichero que no es una imagen se rechaza aunque se llame .png")
    void rejectsNonImageFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "trampa.png", "image/png",
                "contenido que no es una imagen".getBytes(StandardCharsets.UTF_8));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> mediaService.upload(file, propietario));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    /**
     * Guarda un usuario de prueba.
     *
     * @param email correo de la cuenta.
     * @param role  rol de la cuenta.
     * @return el usuario guardado.
     */
    private User guardarUsuario(String email, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("hash-de-prueba");
        user.setFullName("Usuario de prueba");
        user.setRole(role);
        return userRepository.save(user);
    }
}
