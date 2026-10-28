package com.pardorps.pardrops.mail;

/**
 * Error controlado al entregar un correo.
 */
public class MailDeliveryException extends RuntimeException {

    /**
     * @param message detalle del fallo.
     * @param cause   causa original.
     */
    public MailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
