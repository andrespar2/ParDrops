package com.pardorps.pardrops.repository;

import com.pardorps.pardrops.model.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Acceso al contenido binario de las imágenes guardadas en la base de datos.
 */
public interface MediaFileRepository extends JpaRepository<MediaFile, String> {

    /**
     * Busca el contenido de una imagen por su nombre almacenado.
     *
     * @param storedName nombre único del fichero.
     * @return el contenido si existe.
     */
    Optional<MediaFile> findByStoredName(String storedName);
}
