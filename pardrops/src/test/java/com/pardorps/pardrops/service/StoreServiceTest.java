package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.StoreRequest;
import com.pardorps.pardrops.dto.StoreResponse;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.Store;
import com.pardorps.pardrops.model.StoreStatus;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.ProductRepository;
import com.pardorps.pardrops.repository.StoreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la gestión de tiendas en {@link StoreService}.
 */
class StoreServiceTest {

    private final StoreRepository storeRepository = mock(StoreRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);

    private final StoreService storeService = new StoreService(storeRepository, productRepository);

    @Test
    @DisplayName("Genera un slug normalizado a partir del nombre comercial")
    void buildsSlugFromBusinessName() {
        when(storeRepository.existsBySlug("estilo-urbano")).thenReturn(false);

        assertEquals("estilo-urbano", storeService.buildSlug("  Estilo Urbano  "));
    }

    @Test
    @DisplayName("Añade un sufijo cuando el slug ya está en uso")
    void addsSuffixWhenSlugIsTaken() {
        when(storeRepository.existsBySlug("estilo-urbano")).thenReturn(true);
        when(storeRepository.existsBySlug("estilo-urbano-1")).thenReturn(false);

        assertEquals("estilo-urbano-1", storeService.buildSlug("Estilo Urbano"));
    }

    @Test
    @DisplayName("Usa un slug por defecto cuando el nombre no tiene caracteres válidos")
    void fallsBackToDefaultSlug() {
        when(storeRepository.existsBySlug("tienda")).thenReturn(false);

        assertEquals("tienda", storeService.buildSlug("###"));
    }

    @Test
    @DisplayName("Devuelve 404 si la tienda no existe")
    void returnsNotFoundForMissingStore() {
        when(storeRepository.findBySlugWithOwner("no-existe")).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> storeService.findPublicBySlug("no-existe"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    @DisplayName("Solo lista las tiendas activas")
    void listsOnlyActiveStores() {
        User seller = user(1L);
        Store active = store(1L, seller, "estilo-urbano", StoreStatus.ACTIVE);
        Store suspended = store(2L, user(2L), "urbano-suspendido", StoreStatus.SUSPENDED);

        when(storeRepository.findAllByOrderByNameAsc()).thenReturn(List.of(active, suspended));
        when(productRepository.countPublishedBySellerId(1L)).thenReturn(3L);

        List<StoreResponse> stores = storeService.findAllActive();

        assertEquals(1, stores.size());
        assertEquals("estilo-urbano", stores.get(0).slug());
        assertEquals(3L, stores.get(0).productCount());
    }

    @Test
    @DisplayName("Devuelve 404 si el vendedor todavía no tiene tienda")
    void returnsNotFoundWhenSellerHasNoStore() {
        User seller = user(1L);
        when(storeRepository.findBySellerIdWithOwner(1L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> storeService.findOwnStore(seller));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    @DisplayName("Normaliza los datos opcionales al actualizar la tienda")
    void normalizesOptionalFieldsOnUpdate() {
        User seller = user(1L);
        Store store = store(1L, seller, "estilo-urbano", StoreStatus.ACTIVE);
        when(storeRepository.findBySellerId(1L)).thenReturn(Optional.of(store));
        when(storeRepository.save(any(Store.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StoreResponse response = storeService.updateOwnStore(
                seller,
                new StoreRequest("  Hola  ", "  ", null, " contacto@pardrops.com ",
                        "3001112222", "  ", null, null, null));

        assertEquals("Hola", response.description());
        assertNull(response.logoUrl());
        assertEquals("contacto@pardrops.com", response.publicEmail());
        assertEquals("3001112222", response.publicPhone());
        assertNull(response.whatsapp());
        verify(storeRepository).save(store);
    }

    @Test
    @DisplayName("Cambia el estado de la tienda y lo guarda")
    void changesStoreStatus() {
        User seller = user(1L);
        Store store = store(1L, seller, "estilo-urbano", StoreStatus.ACTIVE);
        when(storeRepository.findBySellerId(1L)).thenReturn(Optional.of(store));
        when(storeRepository.save(any(Store.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StoreResponse response = storeService.changeStatus(1L, StoreStatus.SUSPENDED);

        assertEquals(StoreStatus.SUSPENDED, response.status());
        verify(storeRepository).save(store);
    }

    @Test
    @DisplayName("No modifica la tienda si el vendedor no tiene ninguna")
    void doesNotUpdateWhenSellerHasNoStore() {
        User seller = user(1L);
        when(storeRepository.findBySellerId(1L)).thenReturn(Optional.empty());

        assertThrows(
                ApiException.class,
                () -> storeService.updateOwnStore(
                        seller,
                        new StoreRequest("Hola", null, null, null, null, null, null, null, null)));

        verify(storeRepository, never()).save(any(Store.class));
    }

    @Test
    @DisplayName("Actualiza el nombre comercial y el slug únicos de la tienda")
    void updatesNameAndSlug() {
        User seller = user(1L);
        Store store = store(1L, seller, "estilo-urbano", StoreStatus.ACTIVE);
        when(storeRepository.findBySellerId(1L)).thenReturn(Optional.of(store));
        when(storeRepository.existsBySlugAndIdNot("moda-urbana", 1L)).thenReturn(false);
        when(storeRepository.save(any(Store.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StoreResponse response = storeService.updateOwnStore(
                seller,
                new StoreRequest(null, null, null, null, null, null,
                        null, "  Moda Urbana  ", "moda-urbana"));

        assertEquals("Moda Urbana", response.name());
        assertEquals("moda-urbana", response.slug());
        verify(storeRepository).save(store);
    }

    @Test
    @DisplayName("Rechaza un slug que ya usa otra tienda")
    void rejectsDuplicatedSlug() {
        User seller = user(1L);
        Store store = store(1L, seller, "estilo-urbano", StoreStatus.ACTIVE);
        when(storeRepository.findBySellerId(1L)).thenReturn(Optional.of(store));
        when(storeRepository.existsBySlugAndIdNot("moda-urbana", 1L)).thenReturn(true);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> storeService.updateOwnStore(
                        seller,
                        new StoreRequest(null, null, null, null, null, null, null, null, "moda-urbana")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("estilo-urbano", store.getSlug());
        verify(storeRepository, never()).save(any(Store.class));
    }

    @Test
    @DisplayName("Conserva el slug propio sin comprobar la unicidad")
    void keepsOwnSlugWithoutUniquenessCheck() {
        User seller = user(1L);
        Store store = store(1L, seller, "estilo-urbano", StoreStatus.ACTIVE);
        when(storeRepository.findBySellerId(1L)).thenReturn(Optional.of(store));
        when(storeRepository.save(any(Store.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StoreResponse response = storeService.updateOwnStore(
                seller,
                new StoreRequest(null, null, null, null, null, null, null, null, "estilo-urbano"));

        assertEquals("estilo-urbano", response.slug());
        verify(storeRepository, never()).existsBySlugAndIdNot(any(), any());
    }

    /**
     * Construye un usuario de prueba.
     *
     * @param id identificador del usuario.
     * @return el usuario listo para usar.
     */
    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("vendedor" + id + "@pardrops.com");
        user.setPassword("hash");
        user.setFullName("Vendedor " + id);
        user.setRole(Role.SELLER);
        return user;
    }

    /**
     * Construye una tienda de prueba.
     *
     * @param id     identificador de la tienda.
     * @param seller vendedor propietario.
     * @param slug   slug público de la tienda.
     * @param status estado de la tienda.
     * @return la tienda lista para usar.
     */
    private Store store(Long id, User seller, String slug, StoreStatus status) {
        Store store = new Store();
        store.setId(id);
        store.setSeller(seller);
        store.setName("Estilo Urbano");
        store.setSlug(slug);
        store.setStatus(status);
        return store;
    }
}
