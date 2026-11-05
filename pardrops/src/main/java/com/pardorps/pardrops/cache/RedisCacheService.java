package com.pardorps.pardrops.cache;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Caché cache-aside sobre Redis.
 *
 * <p>Se activa explícitamente con {@code app.cache.redis.enabled=true}. Si
 * Redis deja de responder, el adaptador entra en un periodo de enfriamiento y
 * todas las lecturas caen a la base de datos sin propagar la excepción a la
 * petición del usuario.</p>
 */
@Component
@ConditionalOnProperty(name = "app.cache.redis.enabled", havingValue = "true")
public class RedisCacheService implements CacheService {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);

    /** Tiempo que se evita llamar a Redis tras un fallo. */
    private static final Duration FAILURE_COOLDOWN = Duration.ofSeconds(30);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();
    private final AtomicLong puts = new AtomicLong();
    private final AtomicLong evictions = new AtomicLong();
    private final AtomicLong errors = new AtomicLong();

    private volatile Instant unavailableUntil = Instant.EPOCH;
    private volatile boolean available = true;

    public RedisCacheService(
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            @Value("${app.cache.redis.key-prefix:pardrops:}") String keyPrefix
    ) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        // El prefijo se documenta pero las claves ya lo incluyen para que
        // puedan inspeccionarse sin ambigüedad en Redis.
        if (!CacheKeys.ROOT.equals(keyPrefix)) {
            log.info("Prefijo de claves Redis configurado: {}", keyPrefix);
        }
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        if (!isReachable()) {
            misses.incrementAndGet();
            return Optional.empty();
        }
        try {
            String json = redis.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                misses.incrementAndGet();
                return Optional.empty();
            }
            T value = objectMapper.readValue(json, type);
            hits.incrementAndGet();
            return Optional.ofNullable(value);
        } catch (Exception exception) {
            registerFailure("lectura", key, exception);
            misses.incrementAndGet();
            return Optional.empty();
        }
    }

    @Override
    public <T> Optional<List<T>> getList(String key, Class<T> elementType) {
        if (!isReachable()) {
            misses.incrementAndGet();
            return Optional.empty();
        }
        try {
            String json = redis.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                misses.incrementAndGet();
                return Optional.empty();
            }
            JavaType listType = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, elementType);
            List<T> value = objectMapper.readValue(json, listType);
            hits.incrementAndGet();
            return Optional.ofNullable(value);
        } catch (Exception exception) {
            registerFailure("lectura de lista", key, exception);
            misses.incrementAndGet();
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        if (value == null || ttl == null || ttl.isZero() || ttl.isNegative() || !isReachable()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            redis.opsForValue().set(key, json, ttl);
            puts.incrementAndGet();
        } catch (Exception exception) {
            registerFailure("escritura", key, exception);
        }
    }

    @Override
    public void evict(String... keys) {
        if (keys == null || keys.length == 0 || !isReachable()) {
            return;
        }
        try {
            Long removed = redis.delete(Arrays.asList(keys));
            evictions.addAndGet(removed == null ? 0 : removed);
        } catch (Exception exception) {
            registerFailure("invalidación", String.join(",", keys), exception);
        }
    }

    @Override
    public void evictByPrefix(String prefix) {
        if (prefix == null || prefix.isBlank() || !isReachable()) {
            return;
        }
        try {
            Set<String> keys = redis.keys(prefix + "*");
            if (keys == null || keys.isEmpty()) {
                return;
            }
            Long removed = redis.delete(keys);
            evictions.addAndGet(removed == null ? 0 : removed);
        } catch (Exception exception) {
            registerFailure("invalidación por prefijo", prefix, exception);
        }
    }

    @Override
    public CacheStats stats() {
        long totalHits = hits.get();
        long totalMisses = misses.get();
        long total = totalHits + totalMisses;
        long stored = 0;
        if (isReachable()) {
            try {
                Set<String> keys = redis.keys(CacheKeys.ROOT + "*");
                stored = keys == null ? 0 : keys.size();
            } catch (Exception exception) {
                registerFailure("conteo", CacheKeys.ROOT, exception);
            }
        }
        return new CacheStats(
                totalHits,
                totalMisses,
                puts.get(),
                evictions.get(),
                errors.get(),
                total == 0 ? 0.0 : Math.round((totalHits * 10_000.0) / total) / 10_000.0,
                true,
                available,
                stored
        );
    }

    private boolean isReachable() {
        return Instant.now().isAfter(unavailableUntil);
    }

    private void registerFailure(String operation, String key, Exception exception) {
        errors.incrementAndGet();
        if (isReachable()) {
            available = false;
            unavailableUntil = Instant.now().plus(FAILURE_COOLDOWN);
            log.warn("Redis no disponible durante {}. Se usará la base de datos {} s. Clave: {}. Causa: {}",
                    operation, FAILURE_COOLDOWN.toSeconds(), key, exception.getMessage());
        }
    }
}
