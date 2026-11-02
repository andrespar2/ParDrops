package com.pardorps.pardrops.dto;

import com.pardorps.pardrops.model.AdminLevel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear una cuenta administrativa.
 *
 * <p>La cuenta se crea con una contraseña temporal que el administrador
 * principal entrega por un canal seguro; el nuevo administrador puede
 * cambiarla después desde su cuenta.</p>
 *
 * @param email    correo de acceso.
 * @param fullName nombre visible.
 * @param password contraseña temporal (mínimo 8 caracteres).
 * @param level    nivel administrativo (nunca OWNER).
 */
public record CreateAdminRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es válido")
        @Size(max = 180, message = "El correo es demasiado largo")
        String email,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre es demasiado largo")
        String fullName,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
        String password,

        @NotNull(message = "El nivel es obligatorio")
        AdminLevel level
) {
}
