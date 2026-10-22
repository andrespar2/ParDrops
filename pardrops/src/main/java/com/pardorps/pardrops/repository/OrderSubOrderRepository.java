package com.pardorps.pardrops.repository;

import com.pardorps.pardrops.model.OrderSubOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a las partes de los pedidos (subpedidos por tienda).
 */
public interface OrderSubOrderRepository extends JpaRepository<OrderSubOrder, Long> {

    /**
     * Devuelve las partes que pertenecen a un vendedor concreto.
     *
     * @param sellerId identificador del vendedor.
     * @return listado de partes, de la más reciente a la más antigua.
     */
    @Query("select s from OrderSubOrder s left join fetch s.order "
            + "where s.seller.id = :sellerId order by s.createdAt desc")
    List<OrderSubOrder> findBySellerId(@Param("sellerId") Long sellerId);

    /**
     * Busca una parte concreta que pertenezca a un vendedor.
     *
     * <p>La pertenencia se filtra en la propia consulta: si el subpedido es de
     * otro vendedor no se encuentra, de modo que el vendedor no puede ni
     * siquiera saber que existe.</p>
     *
     * @param id      identificador de la parte.
     * @param sellerId identificador del vendedor.
     * @return la parte, si pertenece al vendedor.
     */
    @Query("select s from OrderSubOrder s left join fetch s.order "
            + "where s.id = :id and s.seller.id = :sellerId")
    Optional<OrderSubOrder> findByIdAndSellerId(
            @Param("id") Long id, @Param("sellerId") Long sellerId);

    /**
     * Devuelve las partes de un pedido concreto.
     *
     * @param orderId identificador del pedido.
     * @return listado de partes del pedido.
     */
    @Query("select s from OrderSubOrder s where s.order.id = :orderId order by s.createdAt asc")
    List<OrderSubOrder> findByOrderId(@Param("orderId") Long orderId);
}
