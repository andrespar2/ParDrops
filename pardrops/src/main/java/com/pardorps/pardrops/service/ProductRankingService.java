package com.pardorps.pardrops.service;

import com.pardorps.pardrops.cache.CacheKeys;
import com.pardorps.pardrops.cache.CacheService;
import com.pardorps.pardrops.model.OrderStatus;
import com.pardorps.pardrops.repository.OrderItemRepository;
import com.pardorps.pardrops.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Construye el orden dinámico del catálogo.
 *
 * <p>La puntuación base combina calidad (media bayesiana), novedad,
 * popularidad por compras reales, afinidad con categorías compradas y
 * disponibilidad. El orden final añade un jitter determinista derivado de una
 * semilla: dos recargas generan órdenes distintos, pero todas las páginas de
 * una misma navegación comparten exactamente la misma distribución.</p>
 *
 * <p>Los agregados se cachéan y se reutilizan entre peticiones; nunca se
 * guarda una distribución concreta como si fuera el orden global.</p>
 */
@Service
public class ProductRankingService {

    /** Snapshot ligero de un producto para puntuar sin cargar la entidad. */
    public record RankingSnapshot(
            String id,
            Double rating,
            Integer reviewCount,
            Instant createdAt,
            boolean featured,
            boolean isNew,
            int stock,
            double price,
            Long sellerId,
            long soldCount,
            double novelty,
            Set<String> categories
    ) {
    }

    /** Filtros de la distribución. */
    public record FeedFilter(
            String search,
            String category,
            Double minPrice,
            Double maxPrice,
            boolean onlyNew,
            boolean onlyOnSale
    ) {
        public String signature() {
            return String.format(Locale.ROOT, "%s|%s|%s|%s|%s|%s",
                    normalize(search), normalize(category), minPrice, maxPrice, onlyNew, onlyOnSale);
        }

        private static String normalize(String value) {
            if (value == null) {
                return "";
            }
            return value.trim().toLowerCase(Locale.ROOT);
        }
    }

    private record CachedSnapshots(Instant expiresAt, List<RankingSnapshot> snapshots) {
        boolean valid() {
            return Instant.now().isBefore(expiresAt);
        }
    }

    private record CachedCategories(Instant expiresAt, Set<String> categories) {
        boolean valid() {
            return Instant.now().isBefore(expiresAt);
        }
    }

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final CacheService cacheService;

    private final double qualityWeight;
    private final double noveltyWeight;
    private final double popularityWeight;
    private final double affinityWeight;
    private final double availabilityWeight;
    private final double jitterWeight;
    private final double priorMean;
    private final double priorWeight;
    private final double halfLifeDays;
    private final Duration aggregateTtl;

    private final Map<String, CachedSnapshots> localSnapshots = new ConcurrentHashMap<>();
    private final Map<Long, CachedCategories> localAffinities = new ConcurrentHashMap<>();

    public ProductRankingService(
            ProductRepository productRepository,
            OrderItemRepository orderItemRepository,
            CacheService cacheService,
            @Value("${app.ranking.weights.quality:0.38}") double qualityWeight,
            @Value("${app.ranking.weights.novelty:0.18}") double noveltyWeight,
            @Value("${app.ranking.weights.popularity:0.24}") double popularityWeight,
            @Value("${app.ranking.weights.affinity:0.12}") double affinityWeight,
            @Value("${app.ranking.weights.availability:0.08}") double availabilityWeight,
            @Value("${app.ranking.jitter:0.10}") double jitterWeight,
            @Value("${app.ranking.bayesian-prior-mean:4.0}") double priorMean,
            @Value("${app.ranking.bayesian-prior-weight:5}") double priorWeight,
            @Value("${app.ranking.novelty-half-life-days:30}") double halfLifeDays,
            @Value("${app.ranking.aggregate-ttl-seconds:120}") long aggregateTtlSeconds
    ) {
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
        this.cacheService = cacheService;
        this.qualityWeight = qualityWeight;
        this.noveltyWeight = noveltyWeight;
        this.popularityWeight = popularityWeight;
        this.affinityWeight = affinityWeight;
        this.availabilityWeight = availabilityWeight;
        this.jitterWeight = jitterWeight;
        this.priorMean = priorMean;
        this.priorWeight = priorWeight;
        this.halfLifeDays = Math.max(halfLifeDays, 1.0);
        this.aggregateTtl = Duration.ofSeconds(Math.max(aggregateTtlSeconds, 10));
    }

    /**
     * Genera el orden completo de identificadores para una semilla y filtros.
     *
     * @param filter    filtros del catálogo.
     * @param seed      semilla de la navegación.
     * @param sort      orden explícito: relevance, price-asc, price-desc, recent.
     * @param viewerId  usuario autenticado, o nulo para invitados.
     * @return identificadores ordenados y estables para esa semilla.
     */
    @Transactional(readOnly = true)
    public List<String> rank(FeedFilter filter, String seed, String sort, Long viewerId) {
        List<RankingSnapshot> snapshots = snapshotsFor(filter);
        if (snapshots.isEmpty()) {
            return List.of();
        }

        Set<String> affinityCategories = affinityCategories(viewerId);
        String normalizedSort = sort == null ? "relevance" : sort.trim().toLowerCase(Locale.ROOT);
        long maxSold = snapshots.stream().mapToLong(RankingSnapshot::soldCount).max().orElse(0);

        Comparator<RankingSnapshot> comparator = switch (normalizedSort) {
            case "price-asc" -> Comparator.comparingDouble(RankingSnapshot::price);
            case "price-desc" -> Comparator.comparingDouble(RankingSnapshot::price).reversed();
            case "recent" -> Comparator.comparing(RankingSnapshot::createdAt,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            default -> Comparator.comparingDouble((RankingSnapshot snapshot) ->
                    finalScore(snapshot, seed, affinityCategories, maxSold)).reversed();
        };

        return snapshots.stream()
                .sorted(comparator.thenComparing(RankingSnapshot::id))
                .map(RankingSnapshot::id)
                .toList();
    }

    /**
     * Devuelve el conjunto de categorías que el usuario ha comprado.
     * Si no hay sesión o historial, se usa una recomendación general.
     */
    private Set<String> affinityCategories(Long viewerId) {
        if (viewerId == null) {
            return Set.of();
        }
        CachedCategories cached = localAffinities.get(viewerId);
        if (cached != null && cached.valid()) {
            return cached.categories();
        }
        Set<String> categories = orderItemRepository
                .findPurchasedCategoriesByUser(viewerId)
                .stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        localAffinities.put(viewerId, new CachedCategories(
                Instant.now().plus(Duration.ofMinutes(5)), categories));
        return categories;
    }

    private List<RankingSnapshot> snapshotsFor(FeedFilter filter) {
        String signature = filter.signature();
        CachedSnapshots local = localSnapshots.get(signature);
        if (local != null && local.valid()) {
            return local.snapshots();
        }

        String cacheKey = CacheKeys.of(CacheKeys.PRODUCTS_RANKING, signature);
        var redisCached = cacheService.getList(cacheKey, RankingSnapshot.class);
        if (redisCached.isPresent()) {
            localSnapshots.put(signature, new CachedSnapshots(Instant.now().plus(aggregateTtl),
                    redisCached.get()));
            return redisCached.get();
        }

        List<Object[]> rows = productRepository.findRankingCandidates(
                filter.search() == null ? "" : filter.search().trim(),
                trimToNull(filter.category()),
                filter.minPrice(),
                filter.maxPrice(),
                filter.onlyNew(),
                filter.onlyOnSale());

        Map<String, Long> sold = soldQuantities();
        List<String> ids = rows.stream().map(row -> (String) row[0]).toList();
        Map<String, Set<String>> categories = categoriesFor(ids);

        List<RankingSnapshot> snapshots = new ArrayList<>(rows.size());
        Instant reference = Instant.now();
        for (Object[] row : rows) {
            String id = (String) row[0];
            Instant createdAt = (Instant) row[3];
            snapshots.add(new RankingSnapshot(
                    id,
                    (Double) row[1],
                    (Integer) row[2],
                    createdAt,
                    Boolean.TRUE.equals(row[4]),
                    Boolean.TRUE.equals(row[5]),
                    row[6] == null ? 0 : ((Number) row[6]).intValue(),
                    row[7] == null ? 0.0 : ((Number) row[7]).doubleValue(),
                    row[8] == null ? null : ((Number) row[8]).longValue(),
                    sold.getOrDefault(id, 0L),
                    novelty(createdAt, reference),
                    categories.getOrDefault(id, Set.of())
            ));
        }

        List<RankingSnapshot> immutable = List.copyOf(snapshots);
        localSnapshots.put(signature, new CachedSnapshots(Instant.now().plus(aggregateTtl), immutable));
        cacheService.put(cacheKey, immutable, aggregateTtl);
        return immutable;
    }

    private Map<String, Long> soldQuantities() {
        Map<String, Long> sold = new HashMap<>();
        for (Object[] row : orderItemRepository.findSoldQuantities(OrderStatus.CANCELLED)) {
            if (row[0] == null) {
                continue;
            }
            sold.put((String) row[0], row[1] == null ? 0L : ((Number) row[1]).longValue());
        }
        return sold;
    }

    private Map<String, Set<String>> categoriesFor(List<String> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        Map<String, Set<String>> categories = new HashMap<>();
        for (Object[] row : productRepository.findCategoryPairsByProductIds(productIds)) {
            String id = (String) row[0];
            String category = row[1] == null ? "" : row[1].toString();
            categories.computeIfAbsent(id, key -> new HashSet<>())
                    .add(category.trim().toLowerCase(Locale.ROOT));
        }
        return categories;
    }

    private double finalScore(
            RankingSnapshot snapshot,
            String seed,
            Set<String> affinityCategories,
            long maxSold
    ) {
        double quality = bayesianQuality(snapshot.rating(), snapshot.reviewCount());
        double novelty = snapshot.novelty();
        double popularity = maxSold <= 0 ? 0.0
                : Math.log1p(snapshot.soldCount()) / Math.log1p(maxSold);
        double affinity = affinity(snapshot.categories(), affinityCategories);
        double availability = snapshot.stock() > 0 ? 1.0 : 0.15;

        double base = qualityWeight * quality
                + noveltyWeight * novelty
                + popularityWeight * popularity
                + affinityWeight * affinity
                + availabilityWeight * availability;

        // Pequeño impulso comercial, siempre subordinado a los pesos anteriores.
        if (snapshot.featured()) {
            base += 0.02;
        }
        if (snapshot.isNew()) {
            base += 0.01;
        }

        double finalScore = base + jitter(seed, snapshot.id()) * jitterWeight;
        return finalScore;
    }

    private double bayesianQuality(Double rating, Integer reviewCount) {
        double average = rating == null ? 0.0 : rating;
        double count = reviewCount == null ? 0.0 : reviewCount;
        double bayesian = (average * count + priorMean * priorWeight) / (count + priorWeight);
        return Math.max(0.0, Math.min(1.0, bayesian / 5.0));
    }

    private double novelty(Instant createdAt, Instant now) {
        if (createdAt == null) {
            return 0.0;
        }
        double days = Math.max(0.0, ChronoUnit.MINUTES.between(createdAt, now) / (60.0 * 24.0));
        return Math.exp(-Math.log(2.0) * days / halfLifeDays);
    }

    private double affinity(Set<String> productCategories, Set<String> purchasedCategories) {
        if (purchasedCategories.isEmpty() || productCategories.isEmpty()) {
            return 0.0;
        }
        for (String category : productCategories) {
            if (purchasedCategories.contains(category)) {
                return 1.0;
            }
        }
        return 0.0;
    }

    /**
     * Hash determinista de semilla + producto en el rango [0, 1).
     */
    private double jitter(String seed, String productId) {
        int hash = 17;
        hash = hash * 31 + (seed == null ? 0 : seed.hashCode());
        hash = hash * 31 + (productId == null ? 0 : productId.hashCode());
        // Mezcla de avalancha: ids parecidos no producen jitters correlados.
        hash ^= hash >>> 16;
        hash *= 0x7feb352d;
        hash ^= hash >>> 15;
        hash *= 0x846ca68b;
        hash ^= hash >>> 16;
        return Integer.toUnsignedLong(hash) / 4_294_967_296.0;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Invalida los agregados locales y de Redis.
     */
    public void invalidate() {
        localSnapshots.clear();
        localAffinities.clear();
        cacheService.evictByPrefix(CacheKeys.PRODUCTS_RANKING);
    }

    /**
     * Invalida las afinidades locales de un usuario tras una compra.
     *
     * @param userId identificador del usuario.
     */
    public void invalidateAffinity(Long userId) {
        if (userId != null) {
            localAffinities.remove(userId);
        }
    }
}
