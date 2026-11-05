package com.pardorps.pardrops.repository;

import com.pardorps.pardrops.model.OrderItem;
import com.pardorps.pardrops.model.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositorio de las líneas de pedido, usado para los rankings de ventas.
 */
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Agrupa las unidades vendidas por producto, excluyendo pedidos cancelados.
     *
     * @param cancelled estado cancelado.
     * @param pageable  límite de resultados.
     * @return pares de identificador de producto y unidades vendidas.
     */
    @Query("""
            select i.productId, sum(i.quantity)
            from OrderItem i join i.order o
            where o.status <> :cancelled
            group by i.productId
            order by sum(i.quantity) desc
            """)
    List<Object[]> findBestSellingProductIds(
            @Param("cancelled") OrderStatus cancelled,
            Pageable pageable);

    /**
     * Devuelve las unidades vendidas de todos los productos, excluyendo los
     * pedidos cancelados. Se usa para calcular la popularidad del ranking.
     *
     * @param cancelled estado cancelado.
     * @return filas con identificador de producto y unidades vendidas.
     */
    @Query("""
            select i.productId, sum(i.quantity)
            from OrderItem i join i.order o
            where o.status <> :cancelled
            group by i.productId
            """)
    List<Object[]> findSoldQuantities(@Param("cancelled") OrderStatus cancelled);

    /**
     * Devuelve las categorías de los productos que el usuario ya ha comprado.
     *
     * @param userId identificador del usuario.
     * @return nombres de categoría distintos.
     */
    @Query("""
            select distinct c from OrderItem i
            join i.order o
            join Product p on p.id = i.productId
            join p.categories c
            where o.user.id = :userId
            """)
    List<String> findPurchasedCategoriesByUser(@Param("userId") Long userId);
}
