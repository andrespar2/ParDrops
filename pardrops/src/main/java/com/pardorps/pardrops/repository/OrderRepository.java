package com.pardorps.pardrops.repository;

import com.pardorps.pardrops.model.Order;
import com.pardorps.pardrops.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositorio de acceso a los pedidos.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Obtiene los pedidos de un usuario, del más reciente al más antiguo.
     *
     * @param userId identificador del usuario.
     * @return historial de pedidos del usuario.
     */
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Obtiene todos los pedidos, del más reciente al más antiguo.
     * Pensado para el panel de administración.
     *
     * @return listado completo de pedidos.
     */
    List<Order> findAllByOrderByCreatedAtDesc();

    /**
     * Indica si un usuario tiene al menos un pedido en el estado indicado que
     * contenga el producto dado.
     *
     * <p>Se usa para validar que solo quien ha recibido el producto puede
     * dejar una reseña.</p>
     *
     * @param userId    identificador del usuario.
     * @param productId identificador del producto comprado.
     * @param status    estado que debe tener el pedido.
     * @return {@code true} si existe una compra en ese estado.
     */
    boolean existsByUserIdAndItemsProductIdAndStatus(Long userId, String productId, OrderStatus status);

    /**
     * Suma el importe total de todos los pedidos.
     *
     * <p>Se excluyen los pedidos cancelados para que el resumen del panel refleje
     * los ingresos reales de la plataforma.</p>
     *
     * @return importe total facturado.
     */
    @Query("select coalesce(sum(o.total), 0) from Order o where o.status <> com.pardorps.pardrops.model.OrderStatus.CANCELLED")
    Double sumTotal();

    /**
     * Cuenta los pedidos que no están cerrados ni cancelados.
     *
     * @param statuses estados que no cuentan como trabajo pendiente.
     * @return número de pedidos en curso.
     */
    @Query("select count(o) from Order o where o.status not in :statuses")
    long countPending(@Param("statuses") List<OrderStatus> statuses);
}
