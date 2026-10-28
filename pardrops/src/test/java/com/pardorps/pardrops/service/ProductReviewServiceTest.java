package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.CreateReviewRequest;
import com.pardorps.pardrops.dto.ProductReviewResponse;
import com.pardorps.pardrops.dto.ReviewEligibilityResponse;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.OrderStatus;
import com.pardorps.pardrops.model.AuditAction;
import com.pardorps.pardrops.model.Product;
import com.pardorps.pardrops.model.ProductReview;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.OrderRepository;
import com.pardorps.pardrops.repository.ProductRepository;
import com.pardorps.pardrops.repository.ProductReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la lógica de reseñas de {@link ProductReviewService}.
 */
class ProductReviewServiceTest {

    private static final String PRODUCT_ID = "producto-1";

    private final ProductReviewRepository productReviewRepository =
            mock(ProductReviewRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final ProductReviewService productReviewService = new ProductReviewService(
            productReviewRepository, productRepository, orderRepository, auditLogService);

    @Test
    @DisplayName("Publica la reseña cuando el producto fue entregado y actualiza su valoración")
    void createsReviewWhenProductWasDelivered() {
        User user = user(1L);
        Product product = product();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productReviewRepository.existsByProductIdAndUserId(PRODUCT_ID, 1L))
                .thenReturn(false);
        when(orderRepository.existsByUserIdAndItemsProductIdAndStatus(
                1L, PRODUCT_ID, OrderStatus.DELIVERED)).thenReturn(true);
        when(productReviewRepository.save(any(ProductReview.class)))
                .thenAnswer(invocation -> {
                    ProductReview review = invocation.getArgument(0);
                    review.setId(10L);
                    return review;
                });
        when(productReviewRepository.countByProductId(PRODUCT_ID)).thenReturn(3L);
        when(productReviewRepository.averageRatingByProductId(PRODUCT_ID)).thenReturn(4.0);

        ProductReviewResponse response = productReviewService.createReview(
                PRODUCT_ID, new CreateReviewRequest(4, "Muy buen producto."), user);

        assertEquals(10L, response.id());
        assertEquals(4, response.rating());
        assertEquals("Cliente de Prueba", response.customerName());
        assertEquals(4.0, product.getRating());
        assertEquals(3, product.getReviewCount());
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Rechaza reseñar sin una compra entregada con un 403")
    void rejectsReviewWithoutDeliveredOrder() {
        User user = user(1L);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product()));
        when(productReviewRepository.existsByProductIdAndUserId(PRODUCT_ID, 1L))
                .thenReturn(false);
        when(orderRepository.existsByUserIdAndItemsProductIdAndStatus(
                1L, PRODUCT_ID, OrderStatus.DELIVERED)).thenReturn(false);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> productReviewService.createReview(
                        PRODUCT_ID, new CreateReviewRequest(5, "Excelente."), user)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        // La validación usa siempre el estado DELIVERED, nunca uno intermedio.
        verify(orderRepository).existsByUserIdAndItemsProductIdAndStatus(
                1L, PRODUCT_ID, OrderStatus.DELIVERED);
        verify(productReviewRepository, never()).save(any(ProductReview.class));
    }

    @Test
    @DisplayName("Rechaza una segunda reseña del mismo usuario con un 409")
    void rejectsDuplicateReview() {
        User user = user(1L);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product()));
        when(productReviewRepository.existsByProductIdAndUserId(PRODUCT_ID, 1L))
                .thenReturn(true);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> productReviewService.createReview(
                        PRODUCT_ID, new CreateReviewRequest(5, "Excelente."), user)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(productReviewRepository, never()).save(any(ProductReview.class));
    }

    @Test
    @DisplayName("Rechaza publicar una reseña sin sesión con un 401")
    void rejectsReviewWithoutSession() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> productReviewService.createReview(
                        PRODUCT_ID, new CreateReviewRequest(5, "Excelente."), null)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        verify(productReviewRepository, never()).save(any(ProductReview.class));
    }

    @Test
    @DisplayName("Devuelve 404 al reseñar un producto inexistente")
    void failsWhenProductDoesNotExist() {
        when(productRepository.findById("desconocido")).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> productReviewService.createReview(
                        "desconocido", new CreateReviewRequest(5, "Excelente."), user(1L))
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(productReviewRepository, never()).save(any(ProductReview.class));
    }

    @Test
    @DisplayName("La elegibilidad permite reseñar solo a quien recibió el producto y aún no reseñó")
    void eligibilityReflectsPurchaseAndPreviousReview() {
        User user = user(1L);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product()));
        when(productReviewRepository.existsByProductIdAndUserId(PRODUCT_ID, 1L))
                .thenReturn(false);
        when(orderRepository.existsByUserIdAndItemsProductIdAndStatus(
                1L, PRODUCT_ID, OrderStatus.DELIVERED)).thenReturn(true);

        ReviewEligibilityResponse eligibility =
                productReviewService.getEligibility(PRODUCT_ID, user);

        assertTrue(eligibility.canReview());
        assertFalse(eligibility.hasReviewed());
    }

    @Test
    @DisplayName("La elegibilidad es negativa para un visitante anónimo")
    void eligibilityIsNegativeForAnonymousVisitors() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product()));

        ReviewEligibilityResponse eligibility =
                productReviewService.getEligibility(PRODUCT_ID, null);

        assertFalse(eligibility.canReview());
        assertFalse(eligibility.hasReviewed());
        verify(orderRepository, never()).existsByUserIdAndItemsProductIdAndStatus(
                any(), any(), any());
    }

    @Test
    @DisplayName("El administrador elimina una reseña y recalcula la valoración")
    void adminDeletesReviewAndRecalculatesRating() {
        Product product = product();
        User admin = user(9L);
        ProductReview review = new ProductReview();
        review.setId(5L);
        review.setProduct(product);
        review.setUser(user(1L));
        review.setRating(5);
        review.setComment("Muy buena");

        when(productReviewRepository.findById(5L)).thenReturn(Optional.of(review));
        when(productReviewRepository.countByProductId(PRODUCT_ID)).thenReturn(0L);
        when(productReviewRepository.averageRatingByProductId(PRODUCT_ID)).thenReturn(null);

        productReviewService.deleteReview(admin, 5L);

        verify(productReviewRepository).delete(review);
        verify(productRepository).save(product);
        verify(auditLogService).record(
                eq(admin), eq(AuditAction.REVIEW_DELETED),
                eq("PRODUCT_REVIEW"), eq("5"), anyString());
    }

    /**
     * Crea un usuario de prueba.
     *
     * @param id identificador del usuario.
     * @return el usuario listo para usar en las pruebas.
     */
    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("cliente@pardrops.com");
        user.setFullName("Cliente de Prueba");
        user.setRole(Role.CUSTOMER);
        user.setCreatedAt(Instant.now());
        return user;
    }

    /**
     * Crea un producto de prueba.
     *
     * @return el producto listo para usar en las pruebas.
     */
    private Product product() {
        Product product = new Product();
        product.setId(PRODUCT_ID);
        product.setName("Producto de Prueba");
        product.setSlug("producto-de-prueba");
        product.setDescription("Descripción de prueba");
        product.setPrice(19.99);
        product.setStock(10);
        product.setCreatedAt(Instant.now());
        return product;
    }
}
