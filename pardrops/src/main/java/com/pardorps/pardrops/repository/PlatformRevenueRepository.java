package com.pardorps.pardrops.repository;

import com.pardorps.pardrops.model.PlatformRevenue;
import com.pardorps.pardrops.model.PlatformRevenueType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repositorio de los ingresos propios de ParDrops.
 */
public interface PlatformRevenueRepository extends JpaRepository<PlatformRevenue, Long> {

    /**
     * Busca el ingreso de un pedido para un concepto concreto.
     *
     * @param orderId identificador del pedido.
     * @param type    concepto.
     * @return el ingreso, si ya se registró.
     */
    Optional<PlatformRevenue> findByOrderIdAndType(Long orderId, PlatformRevenueType type);

    /**
     * Suma los ingresos de un concepto.
     *
     * @param type concepto.
     * @return total recaudado (0 si no hay registros).
     */
    @Query("select coalesce(sum(r.amount), 0) from PlatformRevenue r where r.type = :type")
    Double sumByType(@Param("type") PlatformRevenueType type);
}
