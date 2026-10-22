package com.pardorps.pardrops.repository;

import com.pardorps.pardrops.model.OrderSubOrder;
import com.pardorps.pardrops.model.OrderStatus;
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

    /**
     * Cuenta las partes de pedido de un vendedor.
     *
     * @param sellerId identificador del vendedor.
     * @return número de partes recibidas.
     */
    long countBySellerId(Long sellerId);

    /**
     * Suma el importe de las partes de pedido de un vendedor.
     *
     * @param sellerId identificador del vendedor.
     * @return importe total vendido por el vendedor.
     */
    @Query("select coalesce(sum(s.total), 0) from OrderSubOrder s where s.seller.id = :sellerId")
    Double sumTotalBySellerId(@Param("sellerId") Long sellerId);

    /**
     * Cuenta las partes de pedido que todavía no han finalizado.
     *
     * <p>Se consideran pendientes todas las que no están entregadas ni
     * canceladas, para que el panel de administración vea el trabajo en curso.</p>
     *
     * @param statuses estados que no cuentan como trabajo pendiente.
     * @return número de partes pendientes.
     */
    @Query("select count(s) from OrderSubOrder s where s.status not in :statuses")
    long countPending(@Param("statuses") List<OrderStatus> statuses);

    /**
     * Devuelve las partes de pedido ordenadas por importe descendente.
     *
     * <p>Se usa para construir el ranking de tiendas con más ventas del
     * resumen del panel de administración.</p>
     *
     * @return listado de partes ordenado por importe.
     */
    @Query("select s from OrderSubOrder s left join fetch s.order "
            + "where s.seller is not null order by s.total desc")
    List<OrderSubOrder> findAllForRanking();
}
