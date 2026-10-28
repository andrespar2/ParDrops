package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.dto.AdminReviewResponse;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.service.AuthService;
import com.pardorps.pardrops.service.ProductReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Moderación de reseñas desde el panel de administración.
 */
@RestController
@RequestMapping("/api/admin/reviews")
public class AdminReviewController {

    private final ProductReviewService reviewService;
    private final AuthService authService;

    /**
     * @param reviewService servicio de reseñas.
     * @param authService   usuario autenticado.
     */
    public AdminReviewController(ProductReviewService reviewService, AuthService authService) {
        this.reviewService = reviewService;
        this.authService = authService;
    }

    /**
     * Lista todas las reseñas.
     *
     * @return reseñas de la más reciente a la más antigua.
     */
    @GetMapping
    public List<AdminReviewResponse> findAll() {
        return reviewService.findAllForAdmin();
    }

    /**
     * Elimina una reseña y recalcula la valoración del producto.
     *
     * @param id identificador de la reseña.
     * @return 204 sin contenido.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        User admin = authService.getCurrentUser();
        reviewService.deleteReview(admin, id);
        return ResponseEntity.noContent().build();
    }
}
