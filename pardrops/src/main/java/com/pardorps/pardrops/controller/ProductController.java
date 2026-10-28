package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.model.Product;
import com.pardorps.pardrops.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API pública del catálogo de productos.
 *
 * <p>Las lecturas son públicas (para el modo invitado) y solo devuelven los
 * productos publicados. La escritura del catálogo deja de estar aquí: la
 * gestiona el panel de administración ({@code /api/admin/products}) y el panel
 * del vendedor ({@code /api/seller/products}).</p>
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    /**
     * @param productService servicio del catálogo.
     */
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Lista los productos publicados del catálogo.
     *
     * @return 200 con la lista de productos.
     */
    @GetMapping
    public ResponseEntity<List<Product>> list() {
        return ResponseEntity.ok(productService.findPublished());
    }

    /**
     * Devuelve los productos más vendidos y mejor valorados de la portada.
     *
     * @return 200 con los rankings.
     */
    @GetMapping("/highlights")
    public ResponseEntity<com.pardorps.pardrops.dto.ProductHighlightsResponse> highlights() {
        return ResponseEntity.ok(productService.highlights());
    }

    /**
     * Obtiene el detalle de un producto por su slug.
     *
     * @param slug slug del producto.
     * @return 200 con el producto.
     */
    @GetMapping("/{slug}")
    public ResponseEntity<Product> detail(@PathVariable String slug) {
        return ResponseEntity.ok(productService.findBySlug(slug));
    }
}
