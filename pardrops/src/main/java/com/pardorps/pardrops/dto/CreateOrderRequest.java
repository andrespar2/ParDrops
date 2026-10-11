package com.pardorps.pardrops.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Datos necesarios para registrar una compra.
 *
 * <p>Recoge la información del cliente solicitada en el checkout (nombre,
 * correo, teléfono y dirección). El usuario autenticado se deduce del token,
 * no se envía en el cuerpo para que no pueda suplantarse.</p>
 *
 * @param customerName       nombre de quien recibe el pedido.
 * @param customerEmail      correo de contacto.
 * @param customerPhone      teléfono de contacto.
 * @param addressStreet      calle y número.
 * @param addressCity        ciudad.
 * @param addressPostalCode  código postal.
 * @param addressCountry     país.
 * @param notes              comentarios adicionales (opcional).
 * @param items              líneas del pedido (al menos una).
 */
public record CreateOrderRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre es demasiado largo")
        String customerName,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 180, message = "El correo es demasiado largo")
        String customerEmail,

        @NotBlank(message = "El teléfono es obligatorio")
        @Size(max = 30, message = "El teléfono es demasiado largo")
        String customerPhone,

        @NotBlank(message = "La dirección es obligatoria")
        @Size(max = 250, message = "La dirección es demasiado larga")
        String addressStreet,

        @NotBlank(message = "La ciudad es obligatoria")
        @Size(max = 120, message = "La ciudad es demasiado larga")
        String addressCity,

        @NotBlank(message = "El código postal es obligatorio")
        @Size(max = 20, message = "El código postal es demasiado largo")
        String addressPostalCode,

        @NotBlank(message = "El país es obligatorio")
        @Size(max = 120, message = "El país es demasiado largo")
        String addressCountry,

        @Size(max = 500, message = "Las notas son demasiado largas")
        String notes,

        @NotEmpty(message = "El pedido debe contener al menos un producto")
        @Valid
        List<OrderItemRequest> items
) {
}
