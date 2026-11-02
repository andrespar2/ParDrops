package com.pardorps.pardrops.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Envío de un mensaje dentro de una conversación.
 *
 * @param body contenido del mensaje.
 */
public record SendSupportMessageRequest(
        @NotBlank(message = "El mensaje es obligatorio")
        @Size(max = 2000, message = "El mensaje es demasiado largo")
        String body
) {
}
