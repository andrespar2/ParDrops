package com.pardorps.pardrops.dto;

import com.pardorps.pardrops.model.LedgerEntryType;

import java.time.Instant;

/**
 * Movimiento del ledger del vendedor.
 *
 * @param id           identificador.
 * @param type         tipo de movimiento.
 * @param reference    referencia.
 * @param description  descripción.
 * @param amount       importe.
 * @param commission   comisión.
 * @param balanceAfter saldo después del movimiento.
 * @param createdAt    fecha.
 */
public record LedgerEntryResponse(
        Long id,
        LedgerEntryType type,
        String reference,
        String description,
        Double amount,
        Double commission,
        Double balanceAfter,
        Instant createdAt
) {
}
