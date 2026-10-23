package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.ProductRequest;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.AuditAction;
import com.pardorps.pardrops.model.NotificationType;
import com.pardorps.pardrops.model.Product;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.Store;
import com.pardorps.pardrops.model.StoreStatus;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.ProductRepository;
import com.pardorps.pardrops.repository.ProductReviewRepository;
import com.pardorps.pardrops.repository.StoreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de las reglas de propiedad del catálogo en {@link ProductService}.
 *
 * <p>Estas reglas son la base de la seguridad del marketplace: un vendedor
 * solo puede tocar sus productos y un cliente no puede tocar ninguno.</p>
 */
class ProductServiceTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ProductReviewRepository productReviewRepository = mock(ProductReviewRepository.class);
    private final StoreRepository storeRepository = mock(StoreRepository.class);
    private final CategoryService categoryService = mock(CategoryService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final NotificationService notificationService = mock(NotificationService.class);

    private final ProductService productService = new ProductService(
            productRepository,
            productReviewRepository,
            storeRepository,
            categoryService,
            auditLogService,
            notificationService);

    @Test
    @DisplayName("Permite que el vendedor propietario edite su producto")
    void allowsOwnerSellerToUpdate() {
        User seller = user(1L, Role.SELLER);
        Product product = product("prod-1", seller);
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(productRepository.existsBySlugAndIdNot("gorra-urbana", "prod-1")).thenReturn(false);
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Product updated = productService.update("prod-1", request("Gorra Urbana", "gorra-urbana"), seller);

        assertEquals("Gorra Urbana", updated.getName());
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Impide que un vendedor edite el producto de otro vendedor")
    void rejectsUpdateFromAnotherSeller() {
        User owner = user(1L, Role.SELLER);
        User intruder = user(2L, Role.SELLER);
        Product product = product("prod-1", owner);
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> productService.update("prod-1", request("Gorra Urbana", "gorra-urbana"), intruder));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Impide que un cliente edite un producto")
    void rejectsUpdateFromCustomer() {
        User customer = user(3L, Role.CUSTOMER);
        Product product = product("prod-1", user(1L, Role.SELLER));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> productService.update("prod-1", request("Gorra Urbana", "gorra-urbana"), customer));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Permite que el administrador edite cualquier producto")
    void allowsAdminToUpdateAnyProduct() {
        User admin = user(9L, Role.ADMIN);
        Product product = product("prod-1", user(1L, Role.SELLER));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(productRepository.existsBySlugAndIdNot("gorra-urbana", "prod-1")).thenReturn(false);
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Product updated = productService.update("prod-1", request("Gorra Urbana", "gorra-urbana"), admin);

        assertEquals("Gorra Urbana", updated.getName());
    }

    @Test
    @DisplayName("Impide que un vendedor borre el producto de otro vendedor")
    void rejectsDeleteFromAnotherSeller() {
        Product product = product("prod-1", user(1L, Role.SELLER));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> productService.delete("prod-1", user(2L, Role.SELLER)));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verify(productRepository, never()).deleteById(any());
        verify(productReviewRepository, never()).deleteByProductId(any());
    }

    @Test
    @DisplayName("Al borrar el producto elimina también sus reseñas")
    void deletesReviewsWithProduct() {
        User seller = user(1L, Role.SELLER);
        Product product = product("prod-1", seller);
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));

        productService.delete("prod-1", seller);

        verify(productReviewRepository).deleteByProductId("prod-1");
        verify(productRepository).deleteById("prod-1");
    }

    @Test
    @DisplayName("Devuelve 404 al consultar un producto inexistente")
    void returnsNotFoundForMissingProduct() {
        when(productRepository.findById("no-existe")).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> productService.findOwned("no-existe", user(1L, Role.SELLER)));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    @DisplayName("Devuelve el catálogo público de una tienda activa")
    void returnsPublishedProductsOfActiveStore() {
        User seller = user(1L, Role.SELLER);
        Store store = store(seller, "estilo-urbano", StoreStatus.ACTIVE);
        Product product = product("prod-1", seller);

        when(storeRepository.findBySlugWithOwner("estilo-urbano")).thenReturn(Optional.of(store));
        when(productRepository.findPublishedBySellerId(1L)).thenReturn(List.of(product));
        when(storeRepository.findAll()).thenReturn(List.of(store));

        List<Product> products = productService.findPublishedByStoreSlug("estilo-urbano");

        assertEquals(1, products.size());
        assertEquals(1L, products.get(0).getSellerId());
        assertEquals("estilo-urbano", products.get(0).getStoreSlug());
    }

    @Test
    @DisplayName("Oculta el catálogo de una tienda suspendida")
    void hidesCatalogOfSuspendedStore() {
        User seller = user(1L, Role.SELLER);
        Store store = store(seller, "estilo-urbano", StoreStatus.SUSPENDED);
        when(storeRepository.findBySlugWithOwner("estilo-urbano")).thenReturn(Optional.of(store));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> productService.findPublishedByStoreSlug("estilo-urbano"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(productRepository, never()).findPublishedBySellerId(any());
    }

    @Test
    @DisplayName("Valida las categorías al crear un producto")
    void validatesCategoriesOnCreate() {
        when(productRepository.existsBySlug("gorra-urbana")).thenReturn(false);
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        productService.create(request("Gorra Urbana", "gorra-urbana"), null);

        verify(categoryService).validateAssignable(List.of("Accesorios"));
    }

    @Test
    @DisplayName("Avisa al vendedor y audita cuando el administrador oculta su producto")
    void notifiesSellerWhenProductIsHidden() {
        User admin = user(9L, Role.ADMIN);
        User seller = user(1L, Role.SELLER);
        Product product = product("prod-1", seller);
        product.setActive(true);

        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(storeRepository.findAll()).thenReturn(List.of());

        productService.setActive("prod-1", false, admin);

        assertEquals(false, product.isActive());
        verify(notificationService).notify(
                eq(seller), eq(NotificationType.PRODUCT), anyString(), anyString(), anyString());
        verify(auditLogService).record(
                eq(admin), eq(AuditAction.PRODUCT_MODERATED), eq("PRODUCT"), eq("prod-1"), anyString());
    }

    @Test
    @DisplayName("No avisa si la visibilidad no cambia")
    void doesNotNotifyWhenVisibilityIsUnchanged() {
        User admin = user(9L, Role.ADMIN);
        Product product = product("prod-1", user(1L, Role.SELLER));
        product.setActive(true);

        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(storeRepository.findAll()).thenReturn(List.of());

        productService.setActive("prod-1", true, admin);

        verify(notificationService, never()).notify(any(), any(), anyString(), anyString(), anyString());
        verify(auditLogService, never()).record(any(), any(), anyString(), anyString(), anyString());
    }

    /**
     * Construye un usuario de prueba.
     *
     * @param id   identificador del usuario.
     * @param role rol asignado.
     * @return el usuario listo para usar.
     */
    private User user(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail("usuario" + id + "@pardrops.com");
        user.setPassword("hash");
        user.setFullName("Usuario " + id);
        user.setRole(role);
        return user;
    }

    /**
     * Construye un producto de prueba.
     *
     * @param id     identificador del producto.
     * @param seller vendedor propietario.
     * @return el producto listo para usar.
     */
    private Product product(String id, User seller) {
        Product product = new Product();
        product.setId(id);
        product.setName("Gorra");
        product.setSlug("gorra");
        product.setDescription("Gorra de prueba");
        product.setPrice(89000.0);
        product.setStock(10);
        product.setSeller(seller);
        return product;
    }

    /**
     * Construye una tienda de prueba.
     *
     * @param seller  vendedor propietario.
     * @param slug    slug público de la tienda.
     * @param status  estado de la tienda.
     * @return la tienda lista para usar.
     */
    private Store store(User seller, String slug, StoreStatus status) {
        Store store = new Store();
        store.setId(1L);
        store.setSeller(seller);
        store.setName("Estilo Urbano");
        store.setSlug(slug);
        store.setStatus(status);
        return store;
    }

    /**
     * Construye una petición de producto válida.
     *
     * @param name nombre del producto.
     * @param slug slug del producto.
     * @return la petición lista para usar.
     */
    private ProductRequest request(String name, String slug) {
        return new ProductRequest(
                name,
                slug,
                "Descripción del producto",
                "Descripción larga del producto",
                89000.0,
                null,
                "COP",
                List.of(),
                List.of("Accesorios"),
                List.of("gorra"),
                10,
                List.of(),
                4.5,
                0,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE);
    }
}
