package com.pardorps.pardrops.payment;

/**
 * Error al comunicarse con la API de Wompi.
 */
public class WompiClientException extends RuntimeException {

    /** Código HTTP devuelto por Wompi ({@code 0} si no hubo respuesta). */
    private final int statusCode;

    /**
     * @param message    mensaje legible (sin secretos).
     * @param statusCode código HTTP de Wompi.
     */
    public WompiClientException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * @param message mensaje legible.
     * @param cause   causa original.
     */
    public WompiClientException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
    }

    /** @return el código HTTP de Wompi. */
    public int getStatusCode() {
        return statusCode;
    }
}
