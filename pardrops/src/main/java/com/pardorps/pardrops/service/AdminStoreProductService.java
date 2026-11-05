package com.pardorps.pardrops.service;

import com.pardorps.pardrops.cache.CacheKeys;
import com.pardorps.pardrops.cache.CacheService;
import com.pardorps.pardrops.dto.AdminStoreProductSummary;
import com.pardorps.pardrops.dto.PageResponse;
import com.pardorps.pardrops.model.Store;
import com.pardorps.pardrops.repository.ProductRepository;
import com.pardorps.pardrops.repository.StoreRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Resúmenes de tiendas para organizar el catálogo del panel administrativo.
 *
 * <p>Los contadores se obtienen con consultas agregadas, nunca descargando
 * todos los productos. El panel pide primero esta lista y después consulta solo
 * los productos de la tienda seleccionada usando el filtro {@code sellerId}.</p>
 */
@Service
public class AdminStoreProductService {

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final CacheService cacheService;
    private final Duration ttl;

    public AdminStoreProductService(
            StoreRepository storeRepository,
            ProductRepository productRepository,
            CacheService cacheService,
            @Value("${app.cache.ttl.admin:30}") long ttlSeconds
    ) {
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.cacheService = cacheService;
        this.ttl = Duration.ofSeconds(Math.max(ttlSeconds, 5));
    }

    /**
     * Lista las tiendas con su contador de productos.
     *
     * @param search texto por nombre de tienda, slug o vendedor.
     * @param sort   name o products.
     * @param page   página (0-based).
     * @param size   tamaño de página.
     * @return página de resúmenes.
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminStoreProductSummary> summaries(
            String search, String sort, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 50));
        int safePage = Math.max(0, page);
        String normalizedSearch = normalize(search);
        String normalizedSort = normalize(sort);

        List<AdminStoreProductSummary> all = cachedSummaries(normalizedSearch);
        List<AdminStoreProductSummary> filtered = all.stream()
                .filter(item -> matches(item, normalizedSearch))
                .sorted(comparator(normalizedSort))
                .toList();

        int from = Math.min(safePage * safeSize, filtered.size());
        int to = Math.min(from + safeSize, filtered.size());
        int totalPages = filtered.isEmpty() ? 0
                : (int) Math.ceil((double) filtered.size() / safeSize);
        return PageResponse.of(filtered.subList(from, to), safePage, safeSize, filtered.size());
    }

    private List<AdminStoreProductSummary> cachedSummaries(String search) {
        String key = CacheKeys.of(CacheKeys.STORES_SUMMARY, search);
        var cached = cacheService.getList(key, AdminStoreProductSummary.class);
        if (cached.isPresent()) {
            return cached.get();
        }

        Map<Long, Long> totals = counts(productRepository.countGroupedBySeller());
        Map<Long, Long> published = counts(productRepository.countPublishedGroupedBySeller());
        List<AdminStoreProductSummary> result = new ArrayList<>();

        long sellerTotal = totals.values().stream().mapToLong(Long::longValue).sum();
        long sellerPublished = published.values().stream().mapToLong(Long::longValue).sum();
        long platformTotal = Math.max(0, productRepository.count() - sellerTotal);
        long platformPublished = Math.max(0, productRepository.countBySellerIsNullAndActiveTrue());
        if (platformTotal > 0) {
            result.add(new AdminStoreProductSummary(
                    null, null, "Catálogo ParDrops", null, "ParDrops",
                    null, platformTotal, platformPublished, platformTotal - platformPublished));
        }

        for (Store store : storeRepository.findAllWithOwner()) {
            Long sellerId = store.getSeller().getId();
            long total = totals.getOrDefault(sellerId, 0L);
            long active = published.getOrDefault(sellerId, 0L);
            result.add(new AdminStoreProductSummary(
                    store.getId(),
                    sellerId,
                    store.getName(),
                    store.getSlug(),
                    store.getSeller().getFullName(),
                    store.getStatus(),
                    total,
                    active,
                    Math.max(0, total - active)
            ));
        }

        // Elimina el contador propio si no hay productos; no ensucia el panel.
        if (platformTotal == 0) {
            result.removeIf(item -> item.storeId() == null);
        }

        List<AdminStoreProductSummary> immutable = List.copyOf(result);
        cacheService.put(key, immutable, ttl);
        return immutable;
    }

    private Map<Long, Long> counts(List<Object[]> rows) {
        Map<Long, Long> values = new HashMap<>();
        for (Object[] row : rows) {
            if (row[0] == null) {
                continue;
            }
            values.put(((Number) row[0]).longValue(),
                    row[1] == null ? 0L : ((Number) row[1]).longValue());
        }
        return values;
    }

    private boolean matches(AdminStoreProductSummary item, String search) {
        if (search.isEmpty()) {
            return true;
        }
        return contains(item.storeName(), search)
                || contains(item.storeSlug(), search)
                || contains(item.sellerName(), search);
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }

    private Comparator<AdminStoreProductSummary> comparator(String sort) {
        if ("products".equals(sort)) {
            return Comparator.comparingLong(AdminStoreProductSummary::productCount).reversed()
                    .thenComparing(AdminStoreProductSummary::storeName,
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        }
        return Comparator.comparing(AdminStoreProductSummary::storeName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparingLong(AdminStoreProductSummary::productCount).reversed();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
