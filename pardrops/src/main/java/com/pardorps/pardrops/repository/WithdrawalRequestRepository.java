package com.pardorps.pardrops.repository;

import com.pardorps.pardrops.model.WithdrawalRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Acceso a las solicitudes de retiro. */
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {

    /**
     * Solicitudes de un vendedor, de la más reciente a la más antigua.
     *
     * @param sellerId identificador del vendedor.
     * @return solicitudes del vendedor.
     */
    List<WithdrawalRequest> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    /**
     * Todas las solicitudes para administración.
     *
     * @return solicitudes ordenadas por fecha.
     */
    List<WithdrawalRequest> findAllByOrderByCreatedAtDesc();

    /**
     * Comprueba si el vendedor ya tiene una solicitud en curso por el importe.
     *
     * @param sellerId identificador del vendedor.
     * @param amount   importe solicitado.
     * @return {@code true} si hay una solicitud igual sin resolver.
     */
    boolean existsBySellerIdAndAmountAndStatusIn(
            Long sellerId,
            Double amount,
            List<com.pardorps.pardrops.model.WithdrawalStatus> statuses);
}
