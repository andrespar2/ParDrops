package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.dto.StoreRequest;
import com.pardorps.pardrops.dto.StoreResponse;
import com.pardorps.pardrops.model.Product;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.service.AuthService;
import com.pardorps.pardrops.service.ProductService;
import com.pardorps.pardrops.service.StoreService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de las tiendas de los vendedores.
 *
 * <p>Las tiendas son públicas: cualquier visitante puede ver el listado y la
 * ficha de una tienda. La edición del perfil comercial exige el rol SELLER y
 * solo afecta a la tienda del propio vendedor.</p>
 */
@RestController
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreService storeService;
    private final ProductService productService;
    private final AuthService authService;

    /**
     * @param storeService   servicio de tiendas.
     * @param productService servicio del catálogo.
     * @param authService    servicio para obtener el usuario autenticado.
     */
    public StoreController(
            StoreService storeService,
            ProductService productService,
            AuthService authService
    ) {
        this.storeService = storeService;
        this.productService = productService;
        this.authService = authService;
    }

    /**
     * Lista las tiendas activas del marketplace.
     *
     * @return 200 con el listado de tiendas.
     */
    @GetMapping
    public ResponseEntity<List<StoreResponse>> list() {
        return ResponseEntity.ok(storeService.findAllActive());
    }

    /**
     * Devuelve la ficha pública de una tienda.
     *
     * @param slug slug público de la tienda.
     * @return 200 con la tienda.
     */
    @GetMapping("/{slug}")
    public ResponseEntity<StoreResponse> detail(@PathVariable String slug) {
        return ResponseEntity.ok(storeService.findPublicBySlug(slug));
    }

    /**
     * Devuelve las reseñas recientes de los productos de una tienda.
     *
     * @param slug slug público de la tienda.
     * @return 200 con las reseñas.
     */
    @GetMapping("/{slug}/reviews")
    public ResponseEntity<List<com.pardorps.pardrops.dto.ProductReviewResponse>> reviews(
            @PathVariable String slug
    ) {
        return ResponseEntity.ok(storeService.findReviews(slug));
    }

    /**
     * Devuelve el catálogo público de una tienda.
     *
     * <p>Solo se muestran los productos publicados de la tienda. Si la tienda
     * está suspendida, el endpoint responde 404 igual que si no existiera.</p>
     *
     * @param slug slug público de la tienda.
     * @return 200 con los productos publicados de la tienda.
     */
    @GetMapping("/{slug}/products")
    public ResponseEntity<List<Product>> products(@PathVariable String slug) {
        return ResponseEntity.ok(productService.findPublishedByStoreSlug(slug));
    }

    /**
     * Devuelve la tienda del vendedor autenticado.
     *
     * @return 200 con la tienda.
     */
    @GetMapping("/mine")
    public ResponseEntity<StoreResponse> mine() {
        User seller = authService.getCurrentUser();
        return ResponseEntity.ok(storeService.findOwnStore(seller));
    }

    /**
     * Actualiza el perfil comercial de la tienda del vendedor autenticado.
     *
     * @param request datos a actualizar.
     * @return 200 con la tienda actualizada.
     */
    @PutMapping("/mine")
    public ResponseEntity<StoreResponse> updateMine(
            @Valid @RequestBody StoreRequest request
    ) {
        User seller = authService.getCurrentUser();
        return ResponseEntity.ok(storeService.updateOwnStore(seller, request));
    }
}
