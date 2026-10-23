package com.pardorps.pardrops.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos que un cliente envía para solicitar convertirse en vendedor.
 *
 * @param businessName nombre comercial del negocio (obligatorio).
 * @param taxId        NIT o identificación tributaria (opcional).
 * @param description  descripción de los productos que se venderán (opcional).
 * @param phone        teléfono de contacto del negocio (opcional).
 * @param city         ciudad desde la que se envía el producto (opcional).
 */
public record SellerApplicationRequest(

        @NotBlank(message = "El nombre del negocio es obligatorio")
        @Size(max = 120, message = "El nombre del negocio es demasiado largo")
        String businessName,

        @Size(max = 40, message = "El NIT es demasiado largo")
        String taxId,

        @Size(max = 2000, message = "La descripción es demasiado extensa")
        String description,

        @Size(max = 30, message = "El teléfono es demasiado largo")
        String phone,

        @Size(max = 120, message = "La ciudad es demasiado larga")
        String city
) {
}
