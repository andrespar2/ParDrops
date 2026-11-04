package com.pardorps.pardrops.repository;

import com.pardorps.pardrops.model.WompiEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio de los eventos recibidos de Wompi.
 */
public interface WompiEventRepository extends JpaRepository<WompiEvent, Long> {

    /**
     * Busca el evento registrado para una transacción.
     *
     * @param transactionId identificador de la transacción.
     * @return el evento, si ya se recibió.
     */
    Optional<WompiEvent> findByTransactionId(String transactionId);
}
