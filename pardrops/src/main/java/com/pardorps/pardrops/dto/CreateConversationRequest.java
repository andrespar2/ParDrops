package com.pardorps.pardrops.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Apertura de una conversación de soporte.
 *
 * @param subject asunto breve.
 * @param message primer mensaje.
 */
public record CreateConversationRequest(
        @NotBlank(message = "El asunto es obligatorio")
        @Size(max = 160, message = "El asunto es demasiado largo")
        String subject,

        @NotBlank(message = "El mensaje es obligatorio")
        @Size(max = 2000, message = "El mensaje es demasiado largo")
        String message
) {
}
