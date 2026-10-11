package com.pardorps.pardrops.dto;

import com.pardorps.pardrops.model.Role;

/**
 * Representación pública de un usuario (sin datos sensibles).
 *
 * @param id       identificador interno.
 * @param email    correo de la cuenta.
 * @param fullName nombre completo.
 * @param phone    teléfono de contacto.
 * @param role     rol asignado (CUSTOMER o ADMIN).
 */
public record UserResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        Role role
) {
}
