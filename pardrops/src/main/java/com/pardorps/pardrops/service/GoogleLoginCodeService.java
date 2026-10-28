package com.pardorps.pardrops.service;

import com.pardorps.pardrops.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona los códigos temporales que entregan la sesión al frontend después
 * del acceso con Google.
 *
 * <p>El JWT nunca viaja en la URL. El manejador de éxito emite un código opaco
 * de un solo uso y el frontend lo canjea por HTTP. Los códigos caducan y se
 * eliminan al consumirse o al detectar que han expirado.</p>
 */
@Service
public class GoogleLoginCodeService {

    /** Longitud en bytes del identificador aleatorio. */
    private static final int CODE_BYTES = 32;

    private final SecureRandom random = new SecureRandom();

    /** Códigos vigentes indexados por su valor. */
    private final Map<String, Entry> codes = new ConcurrentHashMap<>();

    /** Código vigente de cada cuenta, para invalidar los anteriores. */
    private final Map<Long, String> activeCodeByUser = new ConcurrentHashMap<>();

    /** Vida de un código en segundos. */
    private final long ttlSeconds;

    /**
     * @param ttlSeconds vida del código en segundos.
     */
    public GoogleLoginCodeService(@Value("${app.oauth2.code-ttl-seconds:120}") long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    /**
     * Emite un código nuevo para una cuenta.
     *
     * @param userId identificador de la cuenta autenticada.
     * @return el código opaco que se envía al frontend.
     */
    public String issue(Long userId) {
        purgeExpired();
        byte[] bytes = new byte[CODE_BYTES];
        random.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String previous = activeCodeByUser.put(userId, code);
        if (previous != null) {
            codes.remove(previous);
        }
        codes.put(code, new Entry(userId, Instant.now().plusSeconds(ttlSeconds)));
        return code;
    }

    /**
     * Consume un código y devuelve la cuenta asociada.
     *
     * <p>El código se elimina aunque la validación posterior falle, de modo que
     * no pueda reutilizarse.</p>
     *
     * @param code código recibido del frontend.
     * @return identificador de la cuenta.
     * @throws ApiException si el código no existe o ha caducado.
     */
    public Long consume(String code) {
        if (code == null || code.isBlank()) {
            throw ApiException.unauthorized("El código de acceso no es válido o caducó");
        }
        Entry entry = codes.remove(code);
        if (entry == null || !entry.expiresAt().isAfter(Instant.now())) {
            throw ApiException.unauthorized("El código de acceso no es válido o caducó");
        }
        activeCodeByUser.remove(entry.userId(), code);
        return entry.userId();
    }

    /** Elimina los códigos que ya han caducado y sus índices inversos. */
    private void purgeExpired() {
        Instant now = Instant.now();
        codes.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        activeCodeByUser.values().removeIf(code -> !codes.containsKey(code));
    }

    /**
     * Código vigente y cuenta a la que pertenece.
     *
     * @param userId     cuenta autenticada.
     * @param expiresAt  instante de caducidad.
     */
    private record Entry(Long userId, Instant expiresAt) {
    }
}
