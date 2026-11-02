package com.pardorps.pardrops.model;

/** Tipo de movimiento del ledger del vendedor. */
public enum LedgerEntryType {
    SALE,
    WITHDRAWAL_RESERVED,
    WITHDRAWAL_COMPLETED,
    WITHDRAWAL_RELEASED,
    ADJUSTMENT
}
