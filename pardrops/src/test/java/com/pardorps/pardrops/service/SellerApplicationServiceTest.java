package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.SellerApplicationRequest;
import com.pardorps.pardrops.dto.SellerApplicationResponse;
import com.pardorps.pardrops.dto.SellerApplicationReviewRequest;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.AuditAction;
import com.pardorps.pardrops.model.NotificationType;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.SellerApplication;
import com.pardorps.pardrops.model.SellerApplicationStatus;
import com.pardorps.pardrops.model.Store;
import com.pardorps.pardrops.model.StoreStatus;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.AuditLogRepository;
import com.pardorps.pardrops.repository.SellerApplicationRepository;
import com.pardorps.pardrops.repository.StoreRepository;
import com.pardorps.pardrops.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la solicitud para convertirse en vendedor y de su revisión por
 * parte del administrador.
 */
class SellerApplicationServiceTest {

    private final SellerApplicationRepository sellerApplicationRepository =
            mock(SellerApplicationRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final StoreRepository storeRepository = mock(StoreRepository.class);
    private final StoreService storeService = mock(StoreService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final NotificationService notificationService = mock(NotificationService.class);

    private final SellerApplicationService service = new SellerApplicationService(
            sellerApplicationRepository,
            userRepository,
            storeRepository,
            storeService,
            auditLogService,
            notificationService,
            7);

    @Test
    @DisplayName("Registra la solicitud en estado pendiente y avisa al solicitante")
    void submitsApplicationAsPending() {
        User applicant = user(1L, Role.CUSTOMER);
        when(sellerApplicationRepository.findFirstByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.empty());
        when(sellerApplicationRepository.save(any(SellerApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SellerApplicationResponse response = service.submit(
                applicant,
                new SellerApplicationRequest("Estilo Urbano", "900123", "Ropa urbana", "3001112222", "Bogotá"));

        assertEquals(SellerApplicationStatus.PENDING, response.status());
        assertEquals("Estilo Urbano", response.businessName());
        verify(notificationService).notify(
                eq(applicant),
                eq(NotificationType.SELLER_APPLICATION),
                anyString(),
                anyString(),
                any());
    }

    @Test
    @DisplayName("Impide solicitar de nuevo si ya hay una solicitud pendiente")
    void rejectsSecondPendingApplication() {
        User applicant = user(1L, Role.CUSTOMER);
        SellerApplication pending = application(7L, applicant, SellerApplicationStatus.PENDING);
        when(sellerApplicationRepository.findFirstByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(pending));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.submit(applicant, new SellerApplicationRequest("Estilo Urbano", null, null, null, null)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(sellerApplicationRepository, never()).save(any(SellerApplication.class));
    }

    @Test
    @DisplayName("Impide que un vendedor vuelva a solicitarlo")
    void rejectsApplicationFromExistingSeller() {
        User seller = user(2L, Role.SELLER);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.submit(seller, new SellerApplicationRequest("Estilo Urbano", null, null, null, null)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    @DisplayName("Al aprobar convierte al cliente en vendedor y le crea su tienda")
    void approvesApplicationAndCreatesStore() {
        User applicant = user(1L, Role.CUSTOMER);
        User admin = user(9L, Role.ADMIN);
        SellerApplication application = application(7L, applicant, SellerApplicationStatus.PENDING);

        when(sellerApplicationRepository.findById(7L)).thenReturn(Optional.of(application));
        when(sellerApplicationRepository.save(any(SellerApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(storeRepository.findBySellerId(1L)).thenReturn(Optional.empty());
        when(storeService.buildSlug("Estilo Urbano")).thenReturn("estilo-urbano");
        when(storeRepository.save(any(Store.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SellerApplicationResponse response = service.review(
                7L, admin, new SellerApplicationReviewRequest(Boolean.TRUE, "Documentación correcta"));

        assertEquals(SellerApplicationStatus.APPROVED, response.status());
        assertEquals(Role.SELLER, applicant.getRole());
        assertEquals("Estilo Urbano", response.businessName());

        verify(userRepository).save(applicant);
        verify(storeRepository).save(any(Store.class));
        verify(auditLogService).record(
                eq(admin),
                eq(AuditAction.SELLER_APPLICATION_APPROVED),
                anyString(),
                eq("7"),
                anyString());
        verify(notificationService).notify(
                eq(applicant),
                eq(NotificationType.SELLER_APPLICATION),
                anyString(),
                anyString(),
                any());
    }

    @Test
    @DisplayName("Al aprobar crea la tienda activa con el nombre comercial solicitado")
    void createsActiveStoreWithBusinessName() {
        User applicant = user(1L, Role.CUSTOMER);
        SellerApplication application = application(7L, applicant, SellerApplicationStatus.PENDING);

        when(sellerApplicationRepository.findById(7L)).thenReturn(Optional.of(application));
        when(sellerApplicationRepository.save(any(SellerApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(storeRepository.findBySellerId(1L)).thenReturn(Optional.empty());
        when(storeService.buildSlug("Estilo Urbano")).thenReturn("estilo-urbano");
        when(storeRepository.save(any(Store.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.review(7L, user(9L, Role.ADMIN), new SellerApplicationReviewRequest(Boolean.TRUE, null));

        org.mockito.ArgumentCaptor<Store> captor = org.mockito.ArgumentCaptor.forClass(Store.class);
        verify(storeRepository).save(captor.capture());
        Store store = captor.getValue();
        assertEquals("Estilo Urbano", store.getName());
        assertEquals("estilo-urbano", store.getSlug());
        assertEquals(StoreStatus.ACTIVE, store.getStatus());
        assertEquals(applicant, store.getSeller());
    }

    @Test
    @DisplayName("Exige un motivo para rechazar la solicitud")
    void requiresReasonWhenRejecting() {
        User applicant = user(1L, Role.CUSTOMER);
        SellerApplication application = application(7L, applicant, SellerApplicationStatus.PENDING);
        when(sellerApplicationRepository.findById(7L)).thenReturn(Optional.of(application));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.review(7L, user(9L, Role.ADMIN), new SellerApplicationReviewRequest(Boolean.FALSE, "  ")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(sellerApplicationRepository, never()).save(any(SellerApplication.class));
    }

    @Test
    @DisplayName("Al rechazar notifica el motivo y no cambia el rol del usuario")
    void rejectsApplicationAndKeepsCustomerRole() {
        User applicant = user(1L, Role.CUSTOMER);
        SellerApplication application = application(7L, applicant, SellerApplicationStatus.PENDING);
        when(sellerApplicationRepository.findById(7L)).thenReturn(Optional.of(application));
        when(sellerApplicationRepository.save(any(SellerApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SellerApplicationResponse response = service.review(
                7L, user(9L, Role.ADMIN), new SellerApplicationReviewRequest(Boolean.FALSE, "Faltan datos"));

        assertEquals(SellerApplicationStatus.REJECTED, response.status());
        assertEquals("Faltan datos", response.reviewNotes());
        assertEquals(Role.CUSTOMER, applicant.getRole());
        verify(userRepository, never()).save(any(User.class));
        verify(storeRepository, never()).save(any(Store.class));
        verify(auditLogService).record(
                any(User.class),
                eq(AuditAction.SELLER_APPLICATION_REJECTED),
                anyString(),
                eq("7"),
                anyString());
    }

    @Test
    @DisplayName("Impide revisar dos veces la misma solicitud")
    void rejectsSecondReview() {
        User applicant = user(1L, Role.CUSTOMER);
        SellerApplication application = application(7L, applicant, SellerApplicationStatus.APPROVED);
        when(sellerApplicationRepository.findById(7L)).thenReturn(Optional.of(application));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.review(7L, user(9L, Role.ADMIN), new SellerApplicationReviewRequest(Boolean.TRUE, null)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    @DisplayName("Avisa a los administradores cuando entra una solicitud nueva")
    void notifiesAdministratorsAboutNewApplication() {
        User applicant = user(1L, Role.CUSTOMER);
        User admin = user(9L, Role.ADMIN);

        when(sellerApplicationRepository.findFirstByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.empty());
        when(sellerApplicationRepository.save(any(SellerApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(admin));

        service.submit(applicant, new SellerApplicationRequest("Estilo Urbano", null, null, null, null));

        verify(notificationService).notify(
                eq(admin),
                eq(NotificationType.SELLER_APPLICATION),
                anyString(),
                anyString(),
                any());
    }

    @Test
    @DisplayName("Tras un rechazo espera el plazo definido antes de volver a solicitar")
    void waitsCooldownAfterRejection() {
        User applicant = user(1L, Role.CUSTOMER);
        SellerApplication rejected = application(7L, applicant, SellerApplicationStatus.REJECTED);
        rejected.setReviewedAt(Instant.now().minus(Duration.ofDays(2)));

        when(sellerApplicationRepository.findFirstByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(rejected));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.submit(applicant, new SellerApplicationRequest("Estilo Urbano", null, null, null, null)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(sellerApplicationRepository, never()).save(any(SellerApplication.class));
    }

    @Test
    @DisplayName("Pasado el plazo vuelve a admitir una solicitud rechazada")
    void allowsNewApplicationAfterCooldown() {
        User applicant = user(1L, Role.CUSTOMER);
        SellerApplication rejected = application(7L, applicant, SellerApplicationStatus.REJECTED);
        rejected.setReviewedAt(Instant.now().minus(Duration.ofDays(10)));

        when(sellerApplicationRepository.findFirstByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(rejected));
        when(sellerApplicationRepository.save(any(SellerApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SellerApplicationResponse response = service.submit(
                applicant, new SellerApplicationRequest("Estilo Urbano", null, null, null, null));

        assertEquals(SellerApplicationStatus.PENDING, response.status());
    }

    @Test
    @DisplayName("Filtra el listado de solicitudes por estado")
    void filtersApplicationsByStatus() {
        User applicant = user(1L, Role.CUSTOMER);
        SellerApplication pending = application(7L, applicant, SellerApplicationStatus.PENDING);

        when(sellerApplicationRepository.findByStatusOrderByCreatedAtDesc(SellerApplicationStatus.PENDING))
                .thenReturn(List.of(pending));

        assertEquals(1, service.findAll(SellerApplicationStatus.PENDING).size());
        assertTrue(service.findAll(null).isEmpty());
    }

    @Test
    @DisplayName("Devuelve 404 si la solicitud no existe")
    void returnsNotFoundForMissingApplication() {
        when(sellerApplicationRepository.findById(anyLong())).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.review(99L, user(9L, Role.ADMIN), new SellerApplicationReviewRequest(Boolean.TRUE, null)));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getMessage().contains("99"));
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
     * Construye una solicitud de vendedor de prueba.
     *
     * @param id         identificador de la solicitud.
     * @param applicant  cliente que la envía.
     * @param status     estado de la solicitud.
     * @return la solicitud lista para usar.
     */
    private SellerApplication application(Long id, User applicant, SellerApplicationStatus status) {
        SellerApplication application = new SellerApplication();
        application.setId(id);
        application.setUser(applicant);
        application.setBusinessName("Estilo Urbano");
        application.setStatus(status);
        return application;
    }
}
