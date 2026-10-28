package com.pardorps.pardrops.security;

import com.pardorps.pardrops.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limita los intentos de canje del código de Google por dirección de origen.
 *
 * <p>El endpoint de canje es público por necesidad (el navegador llega a él sin
 * sesión), así que se protege con una ventana deslizante sencilla: un máximo de
 * intentos por minuto para cada IP. Superado el límite se responde 429.</p>
 *
 * <p>El estado vive en memoria y está pensado para una sola instancia. Si el
 * backend se despliega en varias, se sustituirá por un almacén compartido.</p>
 */
@Component
public class GoogleExchangeRateLimiter {

    /** Intentos permitidos dentro de la ventana. */
    private static final int MAX_ATTEMPTS = 10;

    /** Duración de la ventana. */
    private static final Duration WINDOW = Duration.ofMinutes(1);

    /** Número máximo de orígenes que se conservan antes de purgar. */
    private static final int MAX_TRACKED_CLIENTS = 1000;

    private final Map<String, Window> attempts = new ConcurrentHashMap<>();

    /**
     * Registra un intento y rechaza si se supera el límite.
     *
     * @param client identificador del origen (normalmente la IP).
     * @throws ApiException con estado 429 si se supera el límite.
     */
    public void check(String client) {
        String key = client == null || client.isBlank() ? "desconocido" : client;
        Instant now = Instant.now();

        Window window = attempts.compute(key, (ignored, current) -> {
            if (current == null || current.startedAt().plus(WINDOW).isBefore(now)) {
                return new Window(now, new AtomicInteger(1));
            }
            return new Window(current.startedAt(), new AtomicInteger(current.count().incrementAndGet()));
        });

        if (attempts.size() > MAX_TRACKED_CLIENTS) {
            attempts.entrySet().removeIf(entry ->
                    entry.getValue().startedAt().plus(WINDOW).isBefore(now));
        }

        if (window.count().get() > MAX_ATTEMPTS) {
            throw new ApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos de acceso. Espera un minuto y vuelve a probar");
        }
    }

    /**
     * Ventana de intentos de un origen.
     *
     * @param startedAt inicio de la ventana.
     * @param count     intentos acumulados.
     */
    private record Window(Instant startedAt, AtomicInteger count) {
    }
}
