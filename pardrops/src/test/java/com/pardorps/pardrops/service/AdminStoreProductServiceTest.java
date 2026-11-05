package com.pardorps.pardrops.service;

import com.pardorps.pardrops.cache.NoOpCacheService;
import com.pardorps.pardrops.dto.AdminStoreProductSummary;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.Store;
import com.pardorps.pardrops.model.StoreStatus;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.ProductRepository;
import com.pardorps.pardrops.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pruebas de los contadores de tiendas del panel administrativo.
 */
class AdminStoreProductServiceTest {

    private StoreRepository storeRepository;
    private ProductRepository productRepository;
    private AdminStoreProductService service;

    @BeforeEach
    void setUp() {
        storeRepository = mock(StoreRepository.class);
        productRepository = mock(ProductRepository.class);
        service = new AdminStoreProductService(
                storeRepository, productRepository, new NoOpCacheService(), 30);

        User seller = new User();
        seller.setId(1L);
        seller.setFullName("Vendedor Demo");
        seller.setRole(Role.SELLER);

        Store store = new Store();
        store.setId(10L);
        store.setName("Estilo Urbano");
        store.setSlug("estilo-urbano");
        store.setSeller(seller);
        store.setStatus(StoreStatus.ACTIVE);

        when(storeRepository.findAllWithOwner()).thenReturn(List.of(store));
        when(productRepository.countGroupedBySeller())
                .thenReturn(List.<Object[]>of(new Object[]{1L, 10L}));
        when(productRepository.countPublishedGroupedBySeller())
                .thenReturn(List.<Object[]>of(new Object[]{1L, 7L}));
        when(productRepository.count()).thenReturn(12L);
        when(productRepository.countBySellerIsNullAndActiveTrue()).thenReturn(1L);
    }

    @Test
    void includesStoreAndPlatformCounters() {
        var page = service.summaries(null, "products", 0, 12);

        assertEquals(2, page.totalElements());
        AdminStoreProductSummary store = page.content().get(0);
        assertEquals(10L, store.productCount());
        assertEquals(7L, store.publishedCount());
        assertEquals(3L, store.hiddenCount());

        AdminStoreProductSummary platform = page.content().get(1);
        assertEquals("Catálogo ParDrops", platform.storeName());
        assertEquals(2L, platform.productCount());
    }

    @Test
    void paginatesWithoutLoadingAllProducts() {
        var page = service.summaries(null, "name", 1, 1);

        assertEquals(2, page.totalElements());
        assertEquals(1, page.content().size());
        assertEquals(1, page.page());
    }

    @Test
    void filtersBySellerName() {
        var page = service.summaries("vendedor", "name", 0, 12);

        assertEquals(1, page.totalElements());
        assertTrue(page.content().get(0).storeName().contains("Estilo"));
    }
}
