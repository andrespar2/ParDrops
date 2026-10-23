package com.pardorps.pardrops.service;

import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.MediaAsset;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.MediaAssetRepository;
import com.pardorps.pardrops.storage.ImageTypeDetector;
import com.pardorps.pardrops.storage.MediaStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Servicio de las imagenes subidas por los usuarios.
 *
 * <p>Valida el formato y el tamaño del fichero, lo guarda en el almacén de
 * archivos y registra sus metadatos en la base de datos. El binario nunca se
 * guarda en la base de datos.</p>
 *
 * <p>El aislamiento se aplica aqui y no en la interfaz: solo el propietario
 * del fichero o un administrador pueden consultarlo o borrarlo.</p>
 */
@Service
public class MediaService {

    /** Repositorio de las fichas de los ficheros. */
    private final MediaAssetRepository mediaAssetRepository;

    /** Almacén donde se guardan los ficheros. */
    private final MediaStorage mediaStorage;

    /** Tamaño máximo admitido en bytes. */
    private final long maxSizeBytes;

    /** Tipos MIME admitidos. */
    private final Set<String> allowedTypes;

    /**
     * @param mediaAssetRepository repositorio de las fichas de los ficheros.
     * @param mediaStorage         almacén donde se guardan los ficheros.
     * @param maxSizeBytes         tamaño máximo admitido en bytes.
     * @param allowedTypes         tipos MIME admitidos, separados por comas.
     */
    public MediaService(
            MediaAssetRepository mediaAssetRepository,
            MediaStorage mediaStorage,
            @Value("${app.media.max-size-bytes:5242880}") long maxSizeBytes,
            @Value("${app.media.allowed-types:image/jpeg,image/png,image/webp,image/gif}")
            String allowedTypes
    ) {
        this.mediaAssetRepository = mediaAssetRepository;
        this.mediaStorage = mediaStorage;
        this.maxSizeBytes = maxSizeBytes;
        this.allowedTypes = new LinkedHashSet<>(Arrays.stream(allowedTypes.split(","))
                .map(String::trim)
                .filter(type -> !type.isEmpty())
                .toList());
    }

    /**
     * Sube una imagen y registra sus metadatos.
     *
     * @param file  fichero recibido.
     * @param owner usuario que sube la imagen.
     * @return la ficha del recurso guardado.
     * @throws ApiException si el fichero esta vacio, no es una imagen valida,
     *                     tiene un formato no admitido o supera el tamaño maximo.
     */
    @Transactional
    public MediaAsset upload(MultipartFile file, User owner) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("No se ha recibido ningun fichero");
        }

        if (file.getSize() > maxSizeBytes) {
            throw ApiException.badRequest(
                    "La imagen supera el tamaño maximo permitido (" + humanSize(maxSizeBytes) + ")");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw ApiException.badRequest("No se pudo leer el fichero recibido");
        }

        String mimeType = ImageTypeDetector.detect(content);
        if (mimeType == null) {
            throw ApiException.badRequest("El fichero no es una imagen valida");
        }

        if (!allowedTypes.contains(mimeType)) {
            throw ApiException.badRequest(
                    "Formato de imagen no permitido. Se admiten: " + String.join(", ", allowedTypes));
        }

        String storedName = UUID.randomUUID() + "."
                + ImageTypeDetector.extensionFor(mimeType);
        MediaStorage.StoredFile stored = mediaStorage.store(storedName, content);

        MediaAsset asset = new MediaAsset();
        asset.setOriginalName(file.getOriginalFilename() == null
                ? storedName
                : file.getOriginalFilename());
        asset.setStoredName(stored.storedName());
        asset.setPublicPath(stored.publicPath());
        asset.setMimeType(mimeType);
        asset.setSizeBytes(stored.sizeBytes());
        asset.setOwner(owner);
        asset.setCreatedAt(Instant.now());

        return mediaAssetRepository.save(asset);
    }

    /**
     * Lista las imagenes subidas por un usuario.
     *
     * @param user usuario autenticado.
     * @return listado de sus imagenes, de la mas reciente a la mas antigua.
     */
    @Transactional(readOnly = true)
    public List<MediaAsset> findMine(User user) {
        return mediaAssetRepository.findByOwnerIdOrderByCreatedAtDesc(user.getId());
    }

    /**
     * Busca una imagen por su identificador.
     *
     * @param id identificador del recurso.
     * @return la ficha del recurso.
     * @throws ApiException si la imagen no existe (404).
     */
    @Transactional(readOnly = true)
    public MediaAsset findById(Long id) {
        return mediaAssetRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("La imagen no existe: " + id));
    }

    /**
     * Comprueba si un usuario puede administrar una imagen.
     *
     * @param asset ficha del recurso.
     * @param user  usuario autenticado.
     * @return {@code true} si es el propietario o un administrador.
     */
    public boolean canManage(MediaAsset asset, User user) {
        if (user == null) {
            return false;
        }
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        return asset.getOwner() != null
                && asset.getOwner().getId() != null
                && asset.getOwner().getId().equals(user.getId());
    }

    /**
     * Elimina una imagen: borra el fichero del almacen y sus metadatos.
     *
     * @param id   identificador del recurso.
     * @param user usuario autenticado.
     * @throws ApiException si la imagen no existe (404) o el usuario no es su
     *                     propietario ni un administrador (403).
     */
    @Transactional
    public void delete(Long id, User user) {
        MediaAsset asset = findById(id);

        if (!canManage(asset, user)) {
            throw ApiException.forbidden("Esa imagen no es tuya");
        }

        mediaStorage.delete(asset.getStoredName());
        mediaAssetRepository.delete(asset);
    }

    /**
     * Convierte un tamaño en bytes a un texto legible.
     *
     * @param bytes tamaño en bytes.
     * @return texto con la unidad correspondiente.
     */
    private String humanSize(long bytes) {
        if (bytes >= 1024 * 1024) {
            return (bytes / (1024 * 1024)) + " MB";
        }
        if (bytes >= 1024) {
            return (bytes / 1024) + " KB";
        }
        return bytes + " B";
    }
}
