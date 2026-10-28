package com.pardorps.pardrops.dto;

import com.pardorps.pardrops.model.AuthProvider;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.UserStatus;

/**
 * Representación pública de un usuario (sin datos sensibles).
 *
 * @param id       identificador interno.
 * @param email    correo de la cuenta.
 * @param fullName nombre completo.
 * @param phone    teléfono de contacto.
 * @param role     rol asignado (CUSTOMER, SELLER o ADMIN).
 * @param status   estado de la cuenta (ACTIVE o SUSPENDED).
 * @param provider origen de la autenticación (LOCAL o GOOGLE).
 * @param avatarUrl foto de perfil del proveedor externo, si existe.
 */
public record UserResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        Role role,
        UserStatus status,
        AuthProvider provider,
        String avatarUrl
) {
}
