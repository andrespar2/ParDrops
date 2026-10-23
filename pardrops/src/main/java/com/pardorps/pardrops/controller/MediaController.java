package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.dto.MediaAssetResponse;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.MediaAsset;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.service.AuthService;
import com.pardorps.pardrops.service.MediaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

/**
 * API de las imagenes subidas a la plataforma.
 *
 * <p>Permite subir una imagen desde el dispositivo, consultar las que ha
 * subido el usuario y borrar las suyas. El fichero se guarda en el almacen de
 * archivos y se sirve despues como recurso publico.</p>
 */
@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final MediaService mediaService;
    private final AuthService authService;

    /**
     * @param mediaService servicio de multimedia.
     * @param authService  servicio para obtener el usuario autenticado.
     */
    public MediaController(MediaService mediaService, AuthService authService) {
        this.mediaService = mediaService;
        this.authService = authService;
    }

    /**
     * Sube una imagen.
     *
     * @param file fichero con la imagen (campo {@code file}).
     * @return 201 con los datos de la imagen guardada.
     */
    @PostMapping
    public ResponseEntity<MediaAssetResponse> upload(@RequestParam("file") MultipartFile file) {
        User user = authService.getCurrentUser();
        MediaAsset asset = mediaService.upload(file, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(asset));
    }

    /**
     * Lista las imagenes que ha subido el usuario autenticado.
     *
     * @return 200 con el listado de sus imagenes.
     */
    @GetMapping
    public ResponseEntity<List<MediaAssetResponse>> list() {
        User user = authService.getCurrentUser();
        List<MediaAssetResponse> assets = mediaService.findMine(user).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(assets);
    }

    /**
     * Devuelve los datos de una imagen.
     *
     * @param id identificador del recurso.
     * @return 200 con los datos de la imagen.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MediaAssetResponse> findOne(@PathVariable Long id) {
        User user = authService.getCurrentUser();
        MediaAsset asset = mediaService.findById(id);

        if (!mediaService.canManage(asset, user)) {
            throw ApiException.forbidden("Esa imagen no es tuya");
        }

        return ResponseEntity.ok(toResponse(asset));
    }

    /**
     * Elimina una imagen y su fichero.
     *
     * @param id identificador del recurso.
     * @return 204 sin contenido.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        User user = authService.getCurrentUser();
        mediaService.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    /**
     * Convierte una ficha de imagen en su representación publica.
     *
     * @param asset ficha del recurso.
     * @return la respuesta con la URL absoluta del fichero.
     */
    private MediaAssetResponse toResponse(MediaAsset asset) {
        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(asset.getPublicPath())
                .toUriString();
        return new MediaAssetResponse(
                asset.getId(),
                url,
                asset.getPublicPath(),
                asset.getOriginalName(),
                asset.getMimeType(),
                asset.getSizeBytes(),
                asset.getCreatedAt()
        );
    }
}
