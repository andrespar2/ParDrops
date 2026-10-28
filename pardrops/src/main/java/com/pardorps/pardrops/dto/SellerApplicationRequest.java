package com.pardorps.pardrops.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos que un cliente envía para solicitar convertirse en vendedor.
 *
 * <p>Los datos de la tienda son públicos. Los datos legales solo los ve el
 * administrador y el propio solicitante.</p>
 *
 * @param businessName   nombre comercial del negocio (obligatorio).
 * @param taxId          NIT o identificación tributaria (opcional).
 * @param description    descripción de los productos que se venderán.
 * @param phone          teléfono de contacto del negocio.
 * @param city           ciudad desde la que se envía el producto.
 * @param publicEmail    correo público de la tienda.
 * @param publicPhone    teléfono público de la tienda.
 * @param legalName      nombre legal del titular.
 * @param documentType   tipo de documento del titular.
 * @param documentNumber número de documento del titular.
 * @param legalAddress   dirección legal del titular.
 * @param personalPhone  teléfono personal del titular.
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
        String city,

        @Email(message = "El correo público no es válido")
        @Size(max = 180, message = "El correo público es demasiado largo")
        String publicEmail,

        @Size(max = 30, message = "El teléfono público es demasiado largo")
        String publicPhone,

        @Size(max = 120, message = "El nombre legal es demasiado largo")
        String legalName,

        @Size(max = 20, message = "El tipo de documento es demasiado largo")
        String documentType,

        @Size(max = 40, message = "El número de documento es demasiado largo")
        String documentNumber,

        @Size(max = 300, message = "La dirección legal es demasiado larga")
        String legalAddress,

        @Size(max = 30, message = "El teléfono personal es demasiado largo")
        String personalPhone
) {
}
