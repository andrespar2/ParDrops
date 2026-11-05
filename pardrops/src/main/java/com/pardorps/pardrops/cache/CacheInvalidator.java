package com.pardorps.pardrops.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Ejecuta la invalidación de caché solo cuando la escritura se ha confirmado.
 *
 * <p>Invalidar antes del commit abre una ventana en la que otra petición
 * podría volver a llenar la caché con el valor antiguo. Si no hay transacción
 * activa, la acción se ejecuta de inmediato.</p>
 */
@Component
public class CacheInvalidator {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidator.class);

    private final CacheService cacheService;

    public CacheInvalidator(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * Programa una invalidación después del commit.
     *
     * @param action acción a ejecutar.
     */
    public void afterCommit(Runnable action) {
        if (action == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runSafely(action);
                }
            });
        } else {
            runSafely(action);
        }
    }

    /**
     * Invalida las claves de catálogo tras confirmar la escritura.
     */
    public void invalidateCatalog() {
        afterCommit(() -> {
            cacheService.evictByPrefix(CacheKeys.PRODUCTS_PUBLISHED);
            cacheService.evictByPrefix(CacheKeys.PRODUCTS_HIGHLIGHTS);
            cacheService.evictByPrefix(CacheKeys.PRODUCTS_FEED);
            cacheService.evictByPrefix(CacheKeys.PRODUCTS_SEARCH);
            cacheService.evictByPrefix(CacheKeys.PRODUCTS_SELLER);
            cacheService.evictByPrefix(CacheKeys.PRODUCTS_STORE);
            cacheService.evictByPrefix(CacheKeys.PRODUCTS_RANKING);
            cacheService.evictByPrefix(CacheKeys.PRODUCT_DETAIL);
            cacheService.evictByPrefix(CacheKeys.STORES_PUBLIC);
            cacheService.evictByPrefix(CacheKeys.STORES_ADMIN);
            cacheService.evictByPrefix(CacheKeys.STORES_SUMMARY);
        });
    }

    /**
     * Invalida las reseñas de un producto y su ficha.
     *
     * @param productId identificador del producto.
     */
    public void invalidateProductReviews(String productId) {
        afterCommit(() -> {
            cacheService.evict(CacheKeys.REVIEWS_PRODUCT + productId);
            cacheService.evictByPrefix(CacheKeys.PRODUCTS_PUBLISHED);
            cacheService.evictByPrefix(CacheKeys.PRODUCTS_FEED);
            cacheService.evictByPrefix(CacheKeys.PRODUCTS_RANKING);
            cacheService.evictByPrefix(CacheKeys.PRODUCTS_SEARCH);
            cacheService.evictByPrefix(CacheKeys.PRODUCT_DETAIL);
            cacheService.evictByPrefix(CacheKeys.REVIEWS_SELLER);
            cacheService.evictByPrefix(CacheKeys.STORES_PUBLIC);
            cacheService.evictByPrefix(CacheKeys.STORES_SUMMARY);
        });
    }

    private void runSafely(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            // La caché nunca debe tumbar una operación ya confirmada.
            log.warn("No se pudo invalidar la caché: {}", exception.getMessage());
        }
    }
}
