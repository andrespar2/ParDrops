package com.pardorps.pardrops.repository;

import com.pardorps.pardrops.model.SellerBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    /** @return suma de comisiones acumuladas. */
    @Query("select coalesce(sum(b.commissionTotal), 0) from SellerBalance b")
    Double sumCommissionTotal();

    /** @return suma del saldo retenido. */
    @Query("select coalesce(sum(b.pendingAmount), 0) from SellerBalance b")
    Double sumPendingAmount();

    /** @return suma del saldo disponible. */
    @Query("select coalesce(sum(b.availableAmount), 0) from SellerBalance b")
    Double sumAvailableAmount();
}
