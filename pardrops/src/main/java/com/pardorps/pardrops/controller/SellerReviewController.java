package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.dto.SellerReviewResponse;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.service.AuthService;
import com.pardorps.pardrops.service.ProductReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Consulta de reseñas del panel de vendedor.
 *
 * <p>Solo expone {@code GET}. No hay endpoints de creación, edición ni borrado
 * para el rol SELLER: esas operaciones siguen reservadas al comprador que
 * publica su propia reseña y al administrador que modera. La pertenencia se
 * filtra en el servidor con el vendedor autenticado.</p>
 */
@RestController
@RequestMapping("/api/seller/reviews")
public class SellerReviewController {

    private final ProductReviewService productReviewService;
    private final AuthService authService;

    public SellerReviewController(
            ProductReviewService productReviewService,
            AuthService authService
    ) {
        this.productReviewService = productReviewService;
        this.authService = authService;
    }

    /**
     * @return 200 con las reseñas de los productos del vendedor autenticado.
     */
    @GetMapping
    public ResponseEntity<List<SellerReviewResponse>> list() {
        User seller = authService.getCurrentUser();
        return ResponseEntity.ok(productReviewService.findForSeller(seller));
    }
}
