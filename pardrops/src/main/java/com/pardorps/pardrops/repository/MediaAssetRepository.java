package com.pardorps.pardrops.repository;

import com.pardorps.pardrops.model.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de las fichas de los ficheros subidos.
 */
public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {

    /**
     * Lista los ficheros subidos por un usuario, del mas reciente al mas antiguo.
     *
     * @param ownerId identificador del propietario.
     * @return listado de ficheros del usuario.
     */
    List<MediaAsset> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
