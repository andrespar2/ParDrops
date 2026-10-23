package com.pardorps.pardrops.repository;

import com.pardorps.pardrops.model.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a las tiendas de los vendedores.
 */
public interface StoreRepository extends JpaRepository<Store, Long> {

    /**
     * Busca la tienda de un vendedor concreto.
     *
     * @param sellerId identificador del vendedor.
     * @return la tienda del vendedor, si existe.
     */
    Optional<Store> findBySellerId(Long sellerId);

    /**
     * Busca la tienda de un vendedor junto con su propietario.
     *
     * @param sellerId identificador del vendedor.
     * @return la tienda del vendedor con el vendedor cargado, si existe.
     */
    @Query("select s from Store s join fetch s.seller where s.seller.id = :sellerId")
    Optional<Store> findBySellerIdWithOwner(@Param("sellerId") Long sellerId);

    /**
     * Busca una tienda por su slug público con su vendedor cargado.
     *
     * @param slug slug de la tienda.
     * @return la tienda, si existe.
     */
    @Query("select s from Store s join fetch s.seller where s.slug = :slug")
    Optional<Store> findBySlugWithOwner(@Param("slug") String slug);

    /**
     * Devuelve todas las tiendas ordenadas por nombre.
     *
     * @return listado de tiendas.
     */
    List<Store> findAllByOrderByNameAsc();

    /**
     * Comprueba si ya existe una tienda con ese slug.
     *
     * @param slug slug a comprobar.
     * @return {@code true} si ya existe.
     */
    boolean existsBySlug(String slug);
}
