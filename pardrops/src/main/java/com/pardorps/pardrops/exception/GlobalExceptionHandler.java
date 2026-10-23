package com.pardorps.pardrops.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de errores de la API.
 *
 * <p>Convierte las excepciones en respuestas JSON homogéneas
 * ({@link ErrorResponse}) con el código HTTP adecuado, de modo que el frontend
 * siempre reciba un mensaje claro y pueda mostrarlo al usuario.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja los errores de negocio controlados.
     *
     * @param exception excepción lanzada por un servicio.
     * @return respuesta con el estado y el mensaje de la excepción.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException exception) {
        return ResponseEntity
                .status(exception.getStatus())
                .body(ErrorResponse.of(exception.getMessage()));
    }

    /**
     * Maneja los fallos de validación de los datos de entrada.
     *
     * @param exception excepción con el detalle por campo.
     * @return respuesta 400 con un mapa de errores campo → mensaje.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Datos inválidos", errors));
    }

    /**
     * Maneja los cuerpos de petición que no se pueden interpretar (JSON mal
     * formado o un valor que no pertenece a un enumerado, entre otros).
     *
     * @param exception excepción de lectura del cuerpo.
     * @return respuesta 400 indicando que los datos no son válidos.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("El cuerpo de la petición no es válido."));
    }

    /**
     * Maneja el intento de acceso de una cuenta suspendida.
     *
     * <p>Se distingue del error de credenciales porque la respuesta correcta no
     * es "inténtalo de nuevo con otras credenciales", sino que el usuario debe
     * contactar con el administrador.</p>
     *
     * @param exception excepción de cuenta deshabilitada.
     * @return respuesta 403 con un mensaje explicativo.
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(DisabledException exception) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(
                        "Tu cuenta está suspendida. Contacta con el administrador para reactivarla."));
    }

    /**
     * Maneja credenciales incorrectas durante el login.
     *
     * @param exception excepción de autenticación.
     * @return respuesta 401 con un mensaje genérico.
     */
    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException exception) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("Correo o contraseña incorrectos"));
    }

    /**
     * Maneja las peticiones a un fichero que no existe o está fuera del
     * almacén de recursos estáticos.
     *
     * <p>Se responde 404 y no un error del servidor: una imagen borrada (o una
     * URL inventada) es sencillamente un recurso que ya no está.</p>
     *
     * @param exception excepción de recurso estático no encontrado.
     * @return respuesta 404 con un mensaje claro.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleMissingResource(NoResourceFoundException exception) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("El recurso solicitado no existe."));
    }

    /**
     * Red de seguridad para cualquier error no previsto.
     *
     * @param exception excepción inesperada.
     * @return respuesta 500 con un mensaje genérico.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("Ha ocurrido un error inesperado: " + exception.getMessage()));
    }
}
