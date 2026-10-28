package com.pardorps.pardrops.repository;

import com.pardorps.pardrops.model.SellerBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio del saldo de los vendedores.
 */
public interface SellerBalanceRepository extends JpaRepository<SellerBalance, Long> {

    /**
     * Busca el saldo de un vendedor.
     *
     * @param sellerId identificador del vendedor.
     * @return el saldo si existe.
     */
    Optional<SellerBalance> findBySellerId(Long sellerId);
}
