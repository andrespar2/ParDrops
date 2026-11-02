package com.pardorps.pardrops.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Confirmación de la transferencia de un retiro.
 *
 * @param reference referencia de la transferencia.
 */
public record WithdrawalCompleteRequest(
        @NotBlank(message = "La referencia de la transferencia es obligatoria")
        @Size(max = 120) String reference
) {
}
