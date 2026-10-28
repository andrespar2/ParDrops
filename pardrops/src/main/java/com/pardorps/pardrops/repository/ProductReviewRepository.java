package com.pardorps.pardrops.repository;

import com.pardorps.pardrops.model.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositorio de acceso a las reseñas de productos.
 */
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    /**
     * Obtiene las reseñas de un producto, de la más reciente a la más antigua.
     *
     * @param productId identificador del producto.
     * @return reseñas publicadas sobre el producto.
     */
    List<ProductReview> findByProductIdOrderByCreatedAtDesc(String productId);

    /**
     * Indica si un usuario ya ha reseñado un producto.
     *
     * @param productId identificador del producto.
     * @param userId    identificador del usuario.
     * @return {@code true} si ya existe una reseña de ese usuario.
     */
    boolean existsByProductIdAndUserId(String productId, Long userId);

    /**
     * Cuenta las reseñas publicadas sobre un producto.
     *
     * @param productId identificador del producto.
     * @return número de reseñas del producto.
     */
    long countByProductId(String productId);

    /**
     * Calcula la calificación media de un producto.
     *
     * @param productId identificador del producto.
     * @return la media de las calificaciones, o {@code null} si no hay reseñas.
     */
    @Query("select avg(r.rating) from ProductReview r where r.product.id = :productId")
    Double averageRatingByProductId(@Param("productId") String productId);

    /**
     * Calcula la media de estrellas de todos los productos de un vendedor.
     *
     * @param sellerId identificador del vendedor.
     * @return la media o {@code null} si no hay reseñas.
     */
    @Query("select avg(r.rating) from ProductReview r where r.product.seller.id = :sellerId")
    Double averageRatingBySellerId(@Param("sellerId") Long sellerId);

    /**
     * Cuenta las reseñas de todos los productos de un vendedor.
     *
     * @param sellerId identificador del vendedor.
     * @return número de reseñas.
     */
    @Query("select count(r) from ProductReview r where r.product.seller.id = :sellerId")
    long countByProductSellerId(@Param("sellerId") Long sellerId);

    /**
     * Devuelve las reseñas más recientes de los productos de un vendedor.
     *
     * @param sellerId identificador del vendedor.
     * @param pageable límite de resultados.
     * @return reseñas ordenadas por fecha descendente.
     */
    @Query("select r from ProductReview r where r.product.seller.id = :sellerId order by r.createdAt desc")
    List<ProductReview> findRecentBySellerId(
            @Param("sellerId") Long sellerId,
            org.springframework.data.domain.Pageable pageable);

    /**
     * Elimina todas las reseñas de un producto.
     *
     * <p>Se usa al borrar un producto del catálogo para no violar la clave
     * extranjera de la tabla de reseñas.</p>
     *
     * @param productId identificador del producto.
     */
    void deleteByProductId(String productId);
}
