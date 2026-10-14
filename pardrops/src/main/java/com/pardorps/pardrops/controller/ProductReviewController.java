package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.dto.CreateReviewRequest;
import com.pardorps.pardrops.dto.ProductReviewResponse;
import com.pardorps.pardrops.dto.ReviewEligibilityResponse;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.service.AuthService;
import com.pardorps.pardrops.service.ProductReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de reseñas de productos.
 *
 * <p>Leer las reseñas y consultar la elegibilidad es público; publicar una
 * reseña requiere sesión y pasa por la validación de compra del servicio. El
 * autor se toma siempre del token, nunca del cuerpo de la petición.</p>
 */
@RestController
@RequestMapping("/api/products/{productId}/reviews")
public class ProductReviewController {

    private final ProductReviewService productReviewService;
    private final AuthService authService;

    /**
     * @param productReviewService servicio de reseñas.
     * @param authService          servicio para obtener el usuario autenticado.
     */
    public ProductReviewController(
            ProductReviewService productReviewService,
            AuthService authService
    ) {
        this.productReviewService = productReviewService;
        this.authService = authService;
    }

    /**
     * Lista las reseñas de un producto.
     *
     * @param productId identificador del producto.
     * @return 200 con las reseñas del producto.
     */
    @GetMapping
    public ResponseEntity<List<ProductReviewResponse>> list(@PathVariable String productId) {
        return ResponseEntity.ok(productReviewService.getReviews(productId));
    }

    /**
     * Indica si el usuario actual puede reseñar el producto.
     *
     * @param productId identificador del producto.
     * @return 200 con el estado de elegibilidad (falso en ambos campos si no hay sesión).
     */
    @GetMapping("/eligibility")
    public ResponseEntity<ReviewEligibilityResponse> eligibility(@PathVariable String productId) {
        User user = authService.getCurrentUserOrNull();
        return ResponseEntity.ok(productReviewService.getEligibility(productId, user));
    }

    /**
     * Publica una reseña del producto para el usuario autenticado.
     *
     * @param productId identificador del producto.
     * @param request   calificación y comentario.
     * @return 201 con la reseña creada.
     */
    @PostMapping
    public ResponseEntity<ProductReviewResponse> create(
            @PathVariable String productId,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        User user = authService.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productReviewService.createReview(productId, request, user));
    }
}
