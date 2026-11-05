package com.pardorps.pardrops.service;

import com.pardorps.pardrops.cache.NoOpCacheService;
import com.pardorps.pardrops.repository.OrderItemRepository;
import com.pardorps.pardrops.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pruebas del ranking dinámico.
 */
class ProductRankingServiceTest {

    private ProductRepository productRepository;
    private OrderItemRepository orderItemRepository;
    private ProductRankingService service;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        orderItemRepository = mock(OrderItemRepository.class);
        service = new ProductRankingService(
                productRepository,
                orderItemRepository,
                new NoOpCacheService(),
                0.38, 0.18, 0.24, 0.12, 0.08,
                0.50, 4.0, 5, 30, 120
        );

        Instant now = Instant.now();
        when(productRepository.findRankingCandidates(
                anyString(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(List.of(
                        new Object[]{"p1", 5.0, 1, now, false, false, 10, 100.0, 1L},
                        new Object[]{"p2", 4.8, 50, now.minus(10, ChronoUnit.DAYS),
                                true, false, 5, 200.0, 2L},
                        new Object[]{"p3", 4.0, 10, now.minus(1, ChronoUnit.DAYS),
                                false, true, 0, 50.0, 3L}
                ));
        when(orderItemRepository.findSoldQuantities(any()))
                .thenReturn(List.of(
                        new Object[]{"p1", 0L},
                        new Object[]{"p2", 100L},
                        new Object[]{"p3", 5L}
                ));
        when(productRepository.findCategoryPairsByProductIds(any()))
                .thenReturn(List.of(
                        new Object[]{"p1", "ropa"},
                        new Object[]{"p2", "ropa"},
                        new Object[]{"p3", "calzado"}
                ));
    }

    @Test
    void sameSeedProducesSameOrder() {
        var filter = new ProductRankingService.FeedFilter("", null, null, null, false, false);

        List<String> first = service.rank(filter, "seed-a", "relevance", null);
        List<String> second = service.rank(filter, "seed-a", "relevance", null);

        assertEquals(first, second);
        assertEquals(3, first.size());
    }

    @Test
    void differentSeedChangesDistributionWithoutLosingProducts() {
        var filter = new ProductRankingService.FeedFilter("", null, null, null, false, false);

        List<String> first = service.rank(filter, "seed-a", "relevance", null);
        List<String> second = service.rank(filter, "seed-b", "relevance", null);

        assertEquals(3, second.size());
        assertEquals(first.stream().sorted().toList(), second.stream().sorted().toList());

        java.util.Set<List<String>> distributions = new java.util.HashSet<>();
        for (int index = 0; index < 12; index++) {
            distributions.add(service.rank(filter, "seed-" + index, "relevance", null));
        }
        assertTrue(distributions.size() > 1,
                "La semilla debe producir más de una distribución controlada");
    }

    @Test
    void explicitSortKeepsDeterministicOrder() {
        var filter = new ProductRankingService.FeedFilter("", null, null, null, false, false);

        assertEquals(List.of("p3", "p1", "p2"),
                service.rank(filter, "seed-a", "price-asc", null));
    }

    @Test
    void categoryFilterUsesDedicatedQueryWithoutNullLower() {
        when(productRepository.findRankingCandidatesByCategory(
                anyString(), anyString(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{"p1", 5.0, 1, Instant.now(), false, false, 10, 100.0, 1L}
                ));

        var filter = new ProductRankingService.FeedFilter("", "ropa", null, null, false, false);
        List<String> result = service.rank(filter, "seed-a", "relevance", null);

        assertEquals(List.of("p1"), result);
        org.mockito.Mockito.verify(productRepository)
                .findRankingCandidatesByCategory(
                        anyString(), org.mockito.ArgumentMatchers.eq("ropa"),
                        any(), any(), anyBoolean(), anyBoolean());
    }
}
