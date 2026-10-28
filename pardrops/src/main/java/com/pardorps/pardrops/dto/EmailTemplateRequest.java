package com.pardorps.pardrops.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos editables de una plantilla de correo.
 *
 * @param subject  asunto del correo.
 * @param bodyHtml cuerpo HTML con variables.
 * @param active   si la plantilla está activa.
 */
public record EmailTemplateRequest(
        @NotBlank(message = "El asunto es obligatorio")
        @Size(max = 250, message = "El asunto no puede superar 250 caracteres")
        String subject,

        @NotBlank(message = "El cuerpo es obligatorio")
        @Size(max = 20000, message = "El cuerpo no puede superar 20000 caracteres")
        String bodyHtml,

        Boolean active
) {
}
