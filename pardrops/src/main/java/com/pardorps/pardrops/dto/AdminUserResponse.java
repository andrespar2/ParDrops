package com.pardorps.pardrops.dto;

import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.UserStatus;

import java.time.Instant;

/**
 * Representación de una cuenta en el panel de administración.
 *
 * <p>Incluye el estado de la cuenta y la tienda asociada (si el usuario es
 * vendedor) para que el panel pueda listarlas sin llamadas adicionales.</p>
 *
 * @param id        identificador interno.
 * @param email     correo de la cuenta.
 * @param fullName  nombre completo.
 * @param phone     teléfono de contacto.
 * @param role      rol asignado.
 * @param status    estado de la cuenta.
 * @param storeName nombre de la tienda (nulo si no es vendedor).
 * @param createdAt fecha de alta de la cuenta.
 */
public record AdminUserResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        Role role,
        UserStatus status,
        String storeName,
        Instant createdAt
) {
}
