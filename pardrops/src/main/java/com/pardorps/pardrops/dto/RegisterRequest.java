package com.pardorps.pardrops.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos enviados por el cliente para registrar una cuenta nueva.
 *
 * @param email    correo electrónico (será el usuario de acceso).
 * @param password contraseña en texto plano; el servidor la cifra.
 * @param fullName nombre completo de la persona.
 * @param phone    teléfono de contacto (opcional).
 */
public record RegisterRequest(

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 180, message = "El correo es demasiado largo")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, max = 100, message = "La contraseña debe tener entre 6 y 100 caracteres")
        String password,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre es demasiado largo")
        String fullName,

        @Size(max = 30, message = "El teléfono es demasiado largo")
        String phone
) {
}
