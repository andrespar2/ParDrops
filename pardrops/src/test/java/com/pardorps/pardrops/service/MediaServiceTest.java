package com.pardorps.pardrops.service;

import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.MediaAsset;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.MediaAssetRepository;
import com.pardorps.pardrops.storage.ImageTypeDetector;
import com.pardorps.pardrops.storage.MediaStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas del servicio de imagenes ({@link MediaService}).
 *
 * <p>Cubren las validaciones de formato y tamaño y el control de acceso: solo
 * el propietario o un administrador pueden gestionar una imagen.</p>
 */
class MediaServiceTest {

    /** Cabecera de un fichero PNG (firma binaria valida). */
    private static final byte[] PNG_HEADER = {
            (byte) 0x89, 'P', 'N', 'G', (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A,
            0x00, 0x00, 0x00, 0x0D
    };

    /** Cabecera de un fichero JPEG (firma binaria valida). */
    private static final byte[] JPEG_HEADER = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0x00, 0x10, 'J', 'F', 'I', 'F', 0x00, 0x01
    };

    private final MediaAssetRepository repository = mock(MediaAssetRepository.class);
    private final MediaStorage storage = mock(MediaStorage.class);

    private MediaService mediaService;

    /** Prepara el servicio con un tamaño maximo pequeño para poder probarlo. */
    @BeforeEach
    void setUp() {
        mediaService = new MediaService(
                repository,
                storage,
                1024,
                "image/jpeg,image/png,image/webp,image/gif");

        when(storage.store(anyString(), any(byte[].class)))
                .thenAnswer(invocation -> new MediaStorage.StoredFile(
                        invocation.getArgument(0),
                        "/media/" + invocation.getArgument(0),
                        ((byte[]) invocation.getArgument(1)).length));
        when(repository.save(any(MediaAsset.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Guarda la imagen valida y registra sus metadatos")
    void storesValidImage() {
        User owner = user(1L, Role.SELLER);
        MockMultipartFile file = new MockMultipartFile(
                "file", "foto.png", "image/png", PNG_HEADER);

        MediaAsset asset = mediaService.upload(file, owner);

        assertEquals("image/png", asset.getMimeType());
        assertEquals("foto.png", asset.getOriginalName());
        assertEquals(PNG_HEADER.length, asset.getSizeBytes());
        assertEquals(owner, asset.getOwner());
        assertTrue(asset.getStoredName().endsWith(".png"));
        assertTrue(asset.getPublicPath().startsWith("/media/"));
    }

    @Test
    @DisplayName("Rechaza un fichero que no es una imagen")
    void rejectsFileThatIsNotAnImage() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "documento.png", "image/png",
                "esto no es una imagen".getBytes(StandardCharsets.UTF_8));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> mediaService.upload(file, user(1L, Role.SELLER)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(storage, never()).store(anyString(), any(byte[].class));
    }

    @Test
    @DisplayName("Un fichero renombrado con extension de imagen se rechaza igual")
    void rejectsTextFileRenamedAsImage() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "script.jpg", "image/jpeg",
                "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> mediaService.upload(file, user(1L, Role.SELLER)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    @DisplayName("Rechaza un formato no permitido con un mensaje claro")
    void rejectsUnsupportedFormat() {
        MediaService servicioSinGif = new MediaService(repository, storage, 1024, "image/jpeg,image/png");
        MockMultipartFile file = new MockMultipartFile(
                "file", "animacion.gif", "image/gif", "GIF89a........".getBytes(StandardCharsets.US_ASCII));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> servicioSinGif.upload(file, user(1L, Role.SELLER)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("Formato"));
    }

    @Test
    @DisplayName("Rechaza una imagen que supera el tamaño maximo")
    void rejectsImageTooLarge() {
        byte[] grande = new byte[2048];
        System.arraycopy(JPEG_HEADER, 0, grande, 0, JPEG_HEADER.length);
        MockMultipartFile file = new MockMultipartFile("file", "foto.jpg", "image/jpeg", grande);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> mediaService.upload(file, user(1L, Role.SELLER)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("tamaño"));
        verify(storage, never()).store(anyString(), any(byte[].class));
    }

    @Test
    @DisplayName("Rechaza la subida vacia")
    void rejectsEmptyUpload() {
        MockMultipartFile file = new MockMultipartFile("file", "vacio.png", "image/png", new byte[0]);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> mediaService.upload(file, user(1L, Role.SELLER)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    @DisplayName("Solo el propietario puede gestionar la imagen")
    void onlyOwnerCanManageImage() {
        MediaAsset asset = asset(5L, user(1L, Role.SELLER));

        assertTrue(mediaService.canManage(asset, user(1L, Role.SELLER)));
        assertTrue(!mediaService.canManage(asset, user(2L, Role.SELLER)));
    }

    @Test
    @DisplayName("El administrador puede gestionar la imagen de cualquier usuario")
    void adminCanManageAnyImage() {
        MediaAsset asset = asset(5L, user(1L, Role.SELLER));

        assertTrue(mediaService.canManage(asset, user(9L, Role.ADMIN)));
    }

    @Test
    @DisplayName("Un usuario no puede borrar una imagen que no es suya")
    void preventsDeletingAnotherUsersImage() {
        MediaAsset asset = asset(5L, user(1L, Role.SELLER));
        when(repository.findById(5L)).thenReturn(Optional.of(asset));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> mediaService.delete(5L, user(2L, Role.SELLER)));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verify(repository, never()).delete(any(MediaAsset.class));
        verify(storage, never()).delete(anyString());
    }

    @Test
    @DisplayName("El propietario borra su imagen: se elimina el fichero y su ficha")
    void ownerDeletesOwnImage() {
        MediaAsset asset = asset(5L, user(1L, Role.SELLER));
        when(repository.findById(5L)).thenReturn(Optional.of(asset));

        mediaService.delete(5L, user(1L, Role.SELLER));

        verify(storage).delete(asset.getStoredName());
        verify(repository).delete(asset);
    }

    @Test
    @DisplayName("Devuelve 404 al borrar una imagen que no existe")
    void returnsNotFoundForMissingImage() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> mediaService.delete(99L, user(1L, Role.SELLER)));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    @DisplayName("Detecta el tipo real de cada formato admitido")
    void detectsRealImageTypes() {
        assertEquals("image/png", ImageTypeDetector.detect(PNG_HEADER));
        assertEquals("image/jpeg", ImageTypeDetector.detect(JPEG_HEADER));
        assertEquals("image/gif", ImageTypeDetector.detect("GIF89a123456".getBytes(StandardCharsets.US_ASCII)));
        assertEquals("image/webp", ImageTypeDetector.detect("RIFF0000WEBPVP8 ".getBytes(StandardCharsets.US_ASCII)));
        assertEquals(null, ImageTypeDetector.detect("contenido plano".getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Construye un usuario de prueba.
     *
     * @param id   identificador.
     * @param role rol de la cuenta.
     * @return el usuario listo para usar.
     */
    private User user(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail("usuario" + id + "@pardrops.com");
        user.setPassword("hash");
        user.setFullName("Usuario " + id);
        user.setRole(role);
        return user;
    }

    /**
     * Construye una ficha de imagen de prueba.
     *
     * @param id    identificador del recurso.
     * @param owner propietario.
     * @return la ficha lista para usar.
     */
    private MediaAsset asset(Long id, User owner) {
        MediaAsset asset = new MediaAsset();
        asset.setId(id);
        asset.setOwner(owner);
        asset.setOriginalName("foto.png");
        asset.setStoredName("abc.png");
        asset.setPublicPath("/media/abc.png");
        asset.setMimeType("image/png");
        asset.setSizeBytes(12);
        return asset;
    }
}
