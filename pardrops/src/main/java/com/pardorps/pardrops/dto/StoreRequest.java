package com.pardorps.pardrops.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos que el vendedor puede actualizar de su propia tienda.
 *
 * <p>El nombre y el slug son opcionales: si llegan vacíos se conserva el valor
 * actual. El slug debe ser único y solo admite minúsculas, números y guiones,
 * porque forma parte de la URL pública de la tienda.</p>
 *
 * @param description  descripción comercial.
 * @param logoUrl      logotipo de la tienda.
 * @param bannerUrl    portada de la tienda.
 * @param contactEmail correo de contacto.
 * @param contactPhone teléfono de contacto.
 * @param whatsapp     WhatsApp de contacto.
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

        @Size(max = 180, message = "El correo es demasiado largo")
        String contactEmail,

        @Size(max = 30, message = "El teléfono es demasiado largo")
        String contactPhone,

        @Size(max = 30, message = "El WhatsApp es demasiado largo")
        String whatsapp,

        @Size(max = 120, message = "El nombre comercial es demasiado largo")
        String name,

        @Size(max = 140, message = "El slug es demasiado largo")
        @Pattern(
                regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                message = "El slug solo admite minúsculas, números y guiones")
        String slug
) {
}
