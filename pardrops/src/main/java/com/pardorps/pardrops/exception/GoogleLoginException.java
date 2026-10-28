package com.pardorps.pardrops.exception;

/**
 * Error controlado durante el acceso con Google.
 *
 * <p>El manejador de éxito de OAuth2 no puede devolver un JSON de error porque
 * el navegador está en medio de una redirección. Esta excepción transporta un
 * código corto que se añade a la URL del frontend para que muestre el mensaje
 * adecuado.</p>
 */
public class GoogleLoginException extends RuntimeException {

    /** Código que viaja en el parámetro {@code oauth} de la redirección. */
    private final String reason;

    /**
     * @param reason  código corto del motivo (por ejemplo {@code cuenta-suspendida}).
     * @param message detalle para el registro interno.
     */
    public GoogleLoginException(String reason, String message) {
        super(message);
        this.reason = reason;
    }

    /** @return el código del motivo que se muestra en el frontend. */
    public String getReason() {
        return reason;
    }
}
