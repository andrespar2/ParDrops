package com.pardorps.pardrops.service;

import com.pardorps.pardrops.exception.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pruebas del código temporal que entrega la sesión tras el acceso con Google.
 */
class GoogleLoginCodeServiceTest {

    @Test
    @DisplayName("El código se canjea una sola vez")
    void codeCanBeConsumedOnlyOnce() {
        GoogleLoginCodeService service = new GoogleLoginCodeService(120);
        String code = service.issue(42L);

        assertEquals(42L, service.consume(code));
        ApiException error = assertThrows(ApiException.class, () -> service.consume(code));
        assertEquals(401, error.getStatus().value());
    }

    @Test
    @DisplayName("Dos emisiones producen códigos distintos")
    void issuesDifferentCodes() {
        GoogleLoginCodeService service = new GoogleLoginCodeService(120);

        assertNotEquals(service.issue(1L), service.issue(1L));
    }

    @Test
    @DisplayName("Un código caducado no se puede canjear")
    void expiredCodeIsRejected() {
        GoogleLoginCodeService service = new GoogleLoginCodeService(0);
        String code = service.issue(1L);

        ApiException error = assertThrows(ApiException.class, () -> service.consume(code));
        assertEquals(401, error.getStatus().value());
    }

    @Test
    @DisplayName("Emitir un código nuevo invalida el anterior de la misma cuenta")
    void newCodeInvalidatesPreviousOne() {
        GoogleLoginCodeService service = new GoogleLoginCodeService(120);
        String first = service.issue(9L);
        String second = service.issue(9L);

        assertThrows(ApiException.class, () -> service.consume(first));
        assertEquals(9L, service.consume(second));
    }

    @Test
    @DisplayName("Un código vacío o inexistente se rechaza")
    void unknownCodeIsRejected() {
        GoogleLoginCodeService service = new GoogleLoginCodeService(120);

        assertThrows(ApiException.class, () -> service.consume("no-existe"));
        assertThrows(ApiException.class, () -> service.consume("  "));
        assertThrows(ApiException.class, () -> service.consume(null));
    }
}
