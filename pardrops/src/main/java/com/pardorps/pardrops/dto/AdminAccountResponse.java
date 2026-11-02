package com.pardorps.pardrops.dto;

import com.pardorps.pardrops.model.AdminLevel;
import com.pardorps.pardrops.model.UserStatus;

import java.time.Instant;

/**
 * Cuenta administrativa expuesta al administrador principal.
 *
 * @param id        identificador de la cuenta.
 * @param email     correo de acceso.
 * @param fullName  nombre visible.
 * @param level     nivel administrativo.
 * @param status    estado de la cuenta.
 * @param createdAt fecha de alta.
 */
public record AdminAccountResponse(
        Long id,
        String email,
        String fullName,
        AdminLevel level,
        UserStatus status,
        Instant createdAt
) {
}
