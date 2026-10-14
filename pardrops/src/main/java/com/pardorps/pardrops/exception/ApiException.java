package com.pardorps.pardrops.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción de negocio con un código HTTP asociado.
 *
 * <p>Permite lanzar errores controlados desde los servicios (por ejemplo,
 * "producto no encontrado" o "correo ya registrado") y que el manejador global
 * los convierta en una respuesta JSON con el estado adecuado.</p>
 */
public class ApiException extends RuntimeException {

    /** Código HTTP con el que responder. */
    private final HttpStatus status;

    /**
     * @param status  código HTTP a devolver.
     * @param message mensaje descriptivo del error.
     */
    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    /** @return el código HTTP asociado al error. */
    public HttpStatus getStatus() {
        return status;
    }

    /**
     * Atajo para un recurso inexistente (404).
     *
     * @param message mensaje del error.
     * @return la excepción lista para lanzar.
     */
    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    /**
     * Atajo para un conflicto, como un correo o slug ya existente (409).
     *
     * @param message mensaje del error.
     * @return la excepción lista para lanzar.
     */
    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }

    /**
     * Atajo para una petición incorrecta (400).
     *
     * @param message mensaje del error.
     * @return la excepción lista para lanzar.
     */
    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Atajo para un problema de credenciales o permisos (401).
     *
     * @param message mensaje del error.
     * @return la excepción lista para lanzar.
     */
    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, message);
    }

    /**
     * Atajo para una operación que el usuario autenticado no puede realizar
     * (403), como reseñar un producto que no ha comprado.
     *
     * @param message mensaje del error.
     * @return la excepción lista para lanzar.
     */
    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, message);
    }
}
