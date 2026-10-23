package com.pardorps.pardrops.repository;

import com.pardorps.pardrops.model.Store;
import com.pardorps.pardrops.model.StoreStatus;
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

    /**
     * Comprueba si otra tienda (con distinto identificador) usa ya ese slug.
     *
     * <p>Se usa al editar el perfil comercial para permitir conservar el slug
     * propio sin chocar con una comprobación de unicidad.</p>
     *
     * @param slug slug a comprobar.
     * @param id   identificador de la tienda que se está editando.
     * @return {@code true} si otra tienda usa el slug.
     */
    boolean existsBySlugAndIdNot(String slug, Long id);

    /**
     * Devuelve todas las tiendas con su vendedor cargado, ordenadas por nombre.
     *
     * <p>Se usa en el panel de administración, que necesita mostrar datos del
     * vendedor junto a los de la tienda.</p>
     *
     * @return listado de tiendas.
     */
    @Query("select s from Store s join fetch s.seller order by s.name asc")
    List<Store> findAllWithOwner();

    /**
     * Devuelve las tiendas en un estado concreto, ordenadas por nombre.
     *
     * @param status estado de la tienda.
     * @return listado de tiendas.
     */
    @Query("select s from Store s join fetch s.seller "
            + "where s.status = :status order by s.name asc")
    List<Store> findByStatusWithOwner(@Param("status") StoreStatus status);

    /**
     * Cuenta las tiendas que están en un estado concreto.
     *
     * @param status estado de la tienda.
     * @return número de tiendas.
     */
    long countByStatus(StoreStatus status);
}
