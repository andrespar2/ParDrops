package com.pardorps.pardrops.dto;

import jakarta.validation.constraints.Size;

/**
 * Rechazo de una solicitud de retiro.
 *
 * @param notes motivo del rechazo.
 */
public record WithdrawalRejectRequest(
        @Size(max = 500) String notes
) {
}
