package com.pardorps.pardrops.security;

import com.pardorps.pardrops.exception.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pruebas del límite de intentos del canje del código de Google.
 */
class GoogleExchangeRateLimiterTest {

    @Test
    @DisplayName("Permite varios intentos normales desde el mismo origen")
    void allowsNormalAttempts() {
        GoogleExchangeRateLimiter limiter = new GoogleExchangeRateLimiter();

        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                limiter.check("192.168.1.10");
            }
        });
    }

    @Test
    @DisplayName("Bloquea con 429 al superar el límite")
    void blocksAfterLimit() {
        GoogleExchangeRateLimiter limiter = new GoogleExchangeRateLimiter();
        for (int i = 0; i < 10; i++) {
            limiter.check("192.168.1.11");
        }

        ApiException error = assertThrows(ApiException.class,
                () -> limiter.check("192.168.1.11"));

        assertEquals(429, error.getStatus().value());
    }

    @Test
    @DisplayName("El límite es independiente para cada origen")
    void limitIsPerOrigin() {
        GoogleExchangeRateLimiter limiter = new GoogleExchangeRateLimiter();
        for (int i = 0; i < 10; i++) {
            limiter.check("origen-a");
        }

        assertDoesNotThrow(() -> limiter.check("origen-b"));
    }

    @Test
    @DisplayName("Un origen desconocido se agrupa bajo una clave segura")
    void unknownOriginUsesSafeKey() {
        GoogleExchangeRateLimiter limiter = new GoogleExchangeRateLimiter();

        assertDoesNotThrow(() -> limiter.check(null));
        assertDoesNotThrow(() -> limiter.check("  "));
    }
}
