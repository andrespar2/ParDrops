package com.pardorps.pardrops.repository;

import com.pardorps.pardrops.model.SellerLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Acceso al ledger de los vendedores. */
public interface SellerLedgerEntryRepository extends JpaRepository<SellerLedgerEntry, Long> {

    /**
     * Movimientos de un vendedor, del más reciente al más antiguo.
     *
     * @param sellerId identificador del vendedor.
     * @return movimientos del vendedor.
     */
    List<SellerLedgerEntry> findBySellerIdOrderByCreatedAtDesc(Long sellerId);
}
