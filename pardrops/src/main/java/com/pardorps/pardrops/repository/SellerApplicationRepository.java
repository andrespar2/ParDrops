package com.pardorps.pardrops.repository;

import com.pardorps.pardrops.model.SellerApplication;
import com.pardorps.pardrops.model.SellerApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a las solicitudes para convertirse en vendedor.
 */
public interface SellerApplicationRepository extends JpaRepository<SellerApplication, Long> {

    /**
     * Busca la última solicitud enviada por un usuario.
     *
     * @param userId identificador del usuario.
     * @return la solicitud más reciente, si existe.
     */
    Optional<SellerApplication> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Busca la última solicitud aprobada de un usuario.
     *
     * @param userId identificador del usuario.
     * @param status estado de la solicitud.
     * @return la solicitud más reciente con ese estado.
     */
    Optional<SellerApplication> findFirstByUserIdAndStatusOrderByCreatedAtDesc(
            Long userId, SellerApplicationStatus status);

    /**
     * Devuelve las solicitudes en un estado concreto.
     *
     * @param status estado de la solicitud.
     * @return listado de solicitudes, de la más reciente a la más antigua.
     */
    List<SellerApplication> findByStatusOrderByCreatedAtDesc(SellerApplicationStatus status);

    /**
     * Devuelve todas las soluciones, de la más reciente a la más antigua.
     *
     * @return listado de solicitudes.
     */
    List<SellerApplication> findAllByOrderByCreatedAtDesc();

    /**
     * Cuenta las solicitudes que están en un estado concreto.
     *
     * @param status estado de las solicitudes.
     * @return número de solicitudes en ese estado.
     */
    long countByStatus(SellerApplicationStatus status);
}
