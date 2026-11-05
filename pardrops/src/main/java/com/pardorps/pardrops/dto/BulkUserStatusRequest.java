package com.pardorps.pardrops.dto;

import com.pardorps.pardrops.model.UserStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Cambio de estado para varias cuentas.
 *
 * @param ids    cuentas seleccionadas.
 * @param status estado que se quiere aplicar.
 * @param reason motivo opcional.
 */
public record BulkUserStatusRequest(
        @NotEmpty(message = "Selecciona al menos una cuenta")
        @Size(max = 100, message = "Puedes actuar sobre 100 cuentas como máximo")
        List<Long> ids,
        @NotNull(message = "Indica el estado que quieres aplicar")
        UserStatus status,
        String reason
) {
}
