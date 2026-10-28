package com.pardorps.pardrops.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos que el vendedor puede actualizar de su propia tienda.
 *
 * <p>El nombre y el slug son opcionales: si llegan vacíos se conserva el valor
 * actual. Los campos de contacto son los públicos de la tienda; el correo
 * personal de la cuenta no se expone.</p>
 *
 * @param description  descripción comercial.
 * @param logoUrl      logotipo de la tienda.
 * @param bannerUrl    portada de la tienda.
 * @param publicEmail  correo público de la tienda.
 * @param publicPhone  teléfono público de la tienda.
 * @param whatsapp     WhatsApp de contacto.
 * @param city         ciudad de la tienda.
 * @param name         nombre comercial de la tienda (opcional).
 * @param slug         slug público de la tienda (opcional).
 */
public record StoreRequest(

        @Size(max = 1000, message = "La descripción es demasiado extensa")
        String description,

        @Size(max = 500, message = "La URL del logotipo es demasiado larga")
        String logoUrl,

        @Size(max = 500, message = "La URL de la portada es demasiado larga")
        String bannerUrl,

        @Email(message = "El correo público no es válido")
        @Size(max = 180, message = "El correo es demasiado largo")
        String publicEmail,

        @Size(max = 30, message = "El teléfono es demasiado largo")
        String publicPhone,

        @Size(max = 30, message = "El WhatsApp es demasiado largo")
        String whatsapp,

        @Size(max = 120, message = "La ciudad es demasiado larga")
        String city,

        @Size(max = 120, message = "El nombre comercial es demasiado largo")
        String name,

        @Size(max = 140, message = "El slug es demasiado largo")
        @Pattern(
                regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                message = "El slug solo admite minúsculas, números y guiones")
        String slug
) {
}
