package com.pardorps.pardrops.repository;

import com.pardorps.pardrops.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
