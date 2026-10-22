package com.pardorps.pardrops.dto;

import jakarta.validation.constraints.Size;

/**
 * Datos que el vendedor puede actualizar de su propia tienda.
 *
 * @param description  descripción comercial.
 * @param logoUrl      logotipo de la tienda.
 * @param bannerUrl    portada de la tienda.
 * @param contactEmail correo de contacto.
 * @param contactPhone teléfono de contacto.
 * @param whatsapp     WhatsApp de contacto.
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
        String whatsapp
) {
}
