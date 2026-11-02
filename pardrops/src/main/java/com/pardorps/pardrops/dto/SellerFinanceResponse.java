package com.pardorps.pardrops.dto;

import java.util.List;

/**
 * Resumen financiero del vendedor.
 *
 * @param available        saldo disponible para retirar.
 * @param pending          ganancias pendientes de liberación.
 * @param reserved         dinero reservado por solicitudes de retiro.
 * @param withdrawnTotal   total retirado.
 * @param totalSales       total histórico de ventas netas.
 * @param commissionTotal  comisiones cobradas por ParDrops.
 * @param movements        historial de movimientos.
 * @param withdrawals      solicitudes de retiro.
 * @param minWithdrawal    valor mínimo de retiro.
 * @param bankFee          comisión configurada para transferencia bancaria.
 */
public record SellerFinanceResponse(
        Double available,
        Double pending,
        Double reserved,
        Double withdrawnTotal,
        Double totalSales,
        Double commissionTotal,
        List<LedgerEntryResponse> movements,
        List<WithdrawalResponse> withdrawals,
        Double minWithdrawal,
        Double bankFee
) {
}
