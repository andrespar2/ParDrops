package com.pardorps.pardrops.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementación nula usada cuando Redis está desactivado.
 *
 * <p>Permite que el resto del código llame siempre al mismo contrato sin
 * ramas condicionales y sin exigir Redis en desarrollo o en despliegues
 * pequeños.</p>
 */
@Component
@ConditionalOnProperty(name = "app.cache.redis.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpCacheService implements CacheService {

    private final AtomicLong misses = new AtomicLong();
    private final AtomicLong errors = new AtomicLong();

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        misses.incrementAndGet();
        return Optional.empty();
    }

    @Override
    public <T> Optional<List<T>> getList(String key, Class<T> elementType) {
        misses.incrementAndGet();
        return Optional.empty();
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        // Sin Redis no se guarda nada.
    }

    @Override
    public void evict(String... keys) {
        // Sin Redis no hay nada que invalidar.
    }

    @Override
    public void evictByPrefix(String prefix) {
        // Sin Redis no hay nada que invalidar.
    }

    @Override
    public CacheStats stats() {
        return new CacheStats(0, misses.get(), 0, 0, errors.get(), 0.0, false, false, 0);
    }
}
