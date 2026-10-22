package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.AdminSellerResponse;
import com.pardorps.pardrops.dto.AdminUserDetailResponse;
import com.pardorps.pardrops.dto.AdminUserResponse;
import com.pardorps.pardrops.dto.PageResponse;
import com.pardorps.pardrops.dto.UserStatusRequest;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.AuditAction;
import com.pardorps.pardrops.model.NotificationType;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.Store;
import com.pardorps.pardrops.model.StoreStatus;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.model.UserStatus;
import com.pardorps.pardrops.repository.OrderSubOrderRepository;
import com.pardorps.pardrops.repository.ProductRepository;
import com.pardorps.pardrops.repository.StoreRepository;
import com.pardorps.pardrops.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la administración de cuentas en {@link AdminUserService}.
 *
 * <p>Comprueban las reglas que protegen la plataforma: el administrador no
 * puede suspender su propia cuenta, suspender a un vendedor suspende su tienda
 * y la ficha de un usuario incluye su tienda y su actividad.</p>
 */
class AdminUserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final StoreRepository storeRepository = mock(StoreRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final OrderSubOrderRepository subOrderRepository = mock(OrderSubOrderRepository.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final NotificationService notificationService = mock(NotificationService.class);

    private final AdminUserService adminUserService = new AdminUserService(
            userRepository,
            storeRepository,
            productRepository,
            subOrderRepository,
            auditLogService,
            notificationService);

    @Test
    @DisplayName("Impide que el administrador suspenda su propia cuenta")
    void refusesToSuspendOwnAccount() {
        User admin = user(9L, Role.ADMIN);
        when(userRepository.findById(9L)).thenReturn(Optional.of(admin));

        ApiException exception = assertThrows(ApiException.class, () -> adminUserService.changeStatus(
                9L, admin, new UserStatusRequest(UserStatus.SUSPENDED, "Prueba")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Suspende al vendedor, suspende su tienda y le avisa")
    void suspendsSellerAndNotifies() {
        User admin = user(9L, Role.ADMIN);
        User seller = user(1L, Role.SELLER);
        Store store = store(seller, StoreStatus.ACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storeRepository.findBySellerId(1L)).thenReturn(Optional.of(store));
        when(storeRepository.findAll()).thenReturn(List.of(store));

        AdminUserResponse response = adminUserService.changeStatus(
                1L, admin, new UserStatusRequest(UserStatus.SUSPENDED, "Incumplimiento"));

        assertEquals(UserStatus.SUSPENDED, response.status());
        assertEquals(StoreStatus.SUSPENDED, store.getStatus());
        verify(storeRepository).save(store);
        verify(auditLogService).record(any(), eq(AuditAction.USER_SUSPENDED), eq("USER"), eq("1"), any());
        verify(notificationService).notify(eq(seller), eq(NotificationType.ACCOUNT), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Reactiva la cuenta y le avisa")
    void reactivatesAccount() {
        User admin = user(9L, Role.ADMIN);
        User customer = user(2L, Role.CUSTOMER);

        when(userRepository.findById(2L)).thenReturn(Optional.of(customer));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserResponse response = adminUserService.changeStatus(
                2L, admin, new UserStatusRequest(UserStatus.ACTIVE, null));

        assertEquals(UserStatus.ACTIVE, response.status());
        verify(auditLogService).record(any(), eq(AuditAction.USER_REACTIVATED), eq("USER"), eq("2"), any());
        verify(notificationService).notify(eq(customer), eq(NotificationType.ACCOUNT), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Devuelve 404 al cambiar el estado de una cuenta inexistente")
    void returnsNotFoundForMissingAccount() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> adminUserService.changeStatus(
                404L, user(9L, Role.ADMIN), new UserStatusRequest(UserStatus.SUSPENDED, null)));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    @DisplayName("La ficha de un vendedor incluye su tienda y su actividad")
    void detailIncludesStoreAndActivity() {
        User seller = user(1L, Role.SELLER);
        Store store = store(seller, StoreStatus.ACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(storeRepository.findBySellerId(1L)).thenReturn(Optional.of(store));
        when(productRepository.countBySellerId(1L)).thenReturn(5L);
        when(subOrderRepository.countBySellerId(1L)).thenReturn(3L);
        when(subOrderRepository.sumTotalBySellerId(1L)).thenReturn(450000.0);

        AdminUserDetailResponse detail = adminUserService.findDetail(1L);

        assertEquals("estilo-urbano", detail.storeSlug());
        assertEquals(5L, detail.productCount());
        assertEquals(3L, detail.salesCount());
        assertEquals(450000.0, detail.salesTotal());
    }

    @Test
    @DisplayName("La ficha de un cliente no incluye tienda")
    void detailWithoutStoreForCustomer() {
        User customer = user(2L, Role.CUSTOMER);

        when(userRepository.findById(2L)).thenReturn(Optional.of(customer));
        when(storeRepository.findBySellerId(2L)).thenReturn(Optional.empty());
        when(subOrderRepository.sumTotalBySellerId(2L)).thenReturn(null);

        AdminUserDetailResponse detail = adminUserService.findDetail(2L);

        assertNull(detail.storeId());
        assertNull(detail.storeStatus());
        assertEquals(0.0, detail.salesTotal());
    }

    @Test
    @DisplayName("Devuelve 404 al consultar la ficha de una cuenta inexistente")
    void detailReturnsNotFound() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class,
                () -> adminUserService.findDetail(404L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    @DisplayName("Pagina el listado de cuentas")
    void paginatesAccounts() {
        User seller = user(1L, Role.SELLER);
        Store store = store(seller, StoreStatus.ACTIVE);
        Page<User> page = new PageImpl<>(List.of(seller), PageRequest.of(0, 10), 1);

        when(userRepository.searchForAdmin(any(), any(), any(), any())).thenReturn(page);
        when(storeRepository.findAll()).thenReturn(List.of(store));

        PageResponse<AdminUserResponse> response =
                adminUserService.searchPage(null, Role.SELLER, null, 0, 10);

        assertEquals(1, response.content().size());
        assertEquals(1, response.totalElements());
        assertEquals("Estilo Urbano", response.content().get(0).storeName());
    }

    @Test
    @DisplayName("Lista los vendedores con su tienda y sus ventas")
    void listsSellersWithActivity() {
        User seller = user(1L, Role.SELLER);
        Store store = store(seller, StoreStatus.ACTIVE);

        when(storeRepository.findAllWithOwner()).thenReturn(List.of(store));
        when(productRepository.countBySellerId(1L)).thenReturn(4L);
        when(subOrderRepository.countBySellerId(1L)).thenReturn(2L);
        when(subOrderRepository.sumTotalBySellerId(1L)).thenReturn(300000.0);

        List<AdminSellerResponse> sellers = adminUserService.findSellers(null, null);

        assertEquals(1, sellers.size());
        assertEquals("estilo-urbano", sellers.get(0).storeSlug());
        assertEquals(300000.0, sellers.get(0).salesTotal());
    }

    @Test
    @DisplayName("Filtra los vendedores por texto de búsqueda")
    void filtersSellersBySearch() {
        User seller = user(1L, Role.SELLER);
        Store store = store(seller, StoreStatus.ACTIVE);
        when(storeRepository.findAllWithOwner()).thenReturn(List.of(store));

        assertEquals(0, adminUserService.findSellers(null, "otra-tienda").size());
        assertEquals(1, adminUserService.findSellers(null, "estilo").size());
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
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    /**
     * Construye una tienda de prueba.
     *
     * @param seller vendedor propietario.
     * @param status estado de la tienda.
     * @return la tienda lista para usar.
     */
    private Store store(User seller, StoreStatus status) {
        Store store = new Store();
        store.setId(1L);
        store.setSeller(seller);
        store.setName("Estilo Urbano");
        store.setSlug("estilo-urbano");
        store.setStatus(status);
        return store;
    }
}
