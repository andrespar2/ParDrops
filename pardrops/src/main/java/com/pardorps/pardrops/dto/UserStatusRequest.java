package com.pardorps.pardrops.dto;

import jakarta.validation.constraints.NotNull;

import com.pardorps.pardrops.model.UserStatus;

/**
 * Cambio de estado de una cuenta de usuario.
 *
 * @param status nuevo estado de la cuenta.
 * @param reason motivo del cambio (opcional).
 */
public record UserStatusRequest(

        @NotNull(message = "Debes indicar el nuevo estado de la cuenta")
        UserStatus status,

        @jakarta.validation.constraints.Size(max = 300, message = "El motivo es demasiado extenso")
        String reason
) {
}
