package com.pardorps.pardrops.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Petición para canjear el código temporal del acceso con Google.
 *
 * @param code código de un solo uso entregado en la redirección.
 */
public record GoogleExchangeRequest(
        @NotBlank(message = "El código es obligatorio")
        String code
) {
}
