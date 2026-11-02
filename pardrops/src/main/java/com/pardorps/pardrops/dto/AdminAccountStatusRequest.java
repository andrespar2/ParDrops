package com.pardorps.pardrops.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Activación o desactivación de una cuenta administrativa.
 *
 * @param active {@code true} para reactivar, {@code false} para desactivar.
 */
public record AdminAccountStatusRequest(
        @NotNull(message = "El estado es obligatorio")
        Boolean active
) {
}
