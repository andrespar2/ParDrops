package com.pardorps.pardrops.dto;

import com.pardorps.pardrops.model.WithdrawalStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cambio de estado de una solicitud de retiro.
 *
 * @param status nuevo estado.
 * @param notes  observaciones.
 */
public record WithdrawalStatusRequest(
        @NotNull(message = "El estado es obligatorio")
        WithdrawalStatus status,
        @Size(max = 500) String notes
) {
}
