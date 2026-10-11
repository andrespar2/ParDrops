package com.pardorps.pardrops.exception;

import java.util.Map;

/**
 * Cuerpo de error que devuelve la API.
 *
 * @param message mensaje principal, legible para el usuario.
 * @param errors  detalle por campo cuando falla la validación (puede ser nulo).
 */
public record ErrorResponse(String message, Map<String, String> errors) {

    /**
     * Crea una respuesta de error simple, sin detalle por campo.
     *
     * @param message mensaje del error.
     * @return la respuesta lista para serializar.
     */
    public static ErrorResponse of(String message) {
        return new ErrorResponse(message, null);
    }
}
