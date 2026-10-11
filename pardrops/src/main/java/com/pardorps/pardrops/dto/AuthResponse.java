package com.pardorps.pardrops.dto;

/**
 * Respuesta devuelta tras un inicio de sesión o registro correcto.
 *
 * @param token token JWT que el cliente debe adjuntar en las siguientes peticiones.
 * @param user  datos públicos del usuario autenticado.
 */
public record AuthResponse(
        String token,
        UserResponse user
) {
}
