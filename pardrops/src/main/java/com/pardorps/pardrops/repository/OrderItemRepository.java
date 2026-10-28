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
}
