package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.dto.ProductFeedResponse;
import com.pardorps.pardrops.model.Product;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.service.AuthService;
import com.pardorps.pardrops.service.ProductService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
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
    private final AuthService authService;

    /**
     * @param productService servicio del catálogo.
     * @param authService    sesión actual (opcional para personalizar el orden).
     */
    public ProductController(ProductService productService, AuthService authService) {
        this.productService = productService;
        this.authService = authService;
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
     * Devuelve una página del catálogo con distribución dinámica.
     *
     * <p>La semilla debe conservarse entre páginas de una misma navegación.
     * El endpoint no se cachea en CDN para no mezclar distribuciones ni
     * preferencias de usuarios distintos.</p>
     *
     * @param search    texto de búsqueda.
     * @param category  categoría exacta.
     * @param minPrice  precio mínimo.
     * @param maxPrice  precio máximo.
     * @param onlyNew   solo novedades.
     * @param onlyOnSale solo ofertas.
     * @param sort      relevance, price-asc, price-desc o recent.
     * @param seed      semilla de distribución.
     * @param page      página solicitada.
     * @param size      tamaño de página.
     * @return 200 con la página y la semilla utilizada.
     */
    @GetMapping("/feed")
    public ResponseEntity<ProductFeedResponse> feed(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(name = "new", required = false, defaultValue = "false") boolean onlyNew,
            @RequestParam(required = false, defaultValue = "false") boolean sale,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String seed,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "24") int size
    ) {
        User viewer = authService.getCurrentUserOrNull();
        ProductFeedResponse response = productService.feed(
                search, category, minPrice, maxPrice, onlyNew, sale,
                sort, seed, viewer == null ? null : viewer.getId(), page, size);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("Vary", "Authorization")
                .body(response);
    }

    /**
     * Resuelve productos concretos por identificador (favoritos).
     *
     * @param ids lista separada por comas.
     * @return 200 con los productos publicados encontrados.
     */
    @GetMapping("/by-ids")
    public ResponseEntity<List<Product>> byIds(@RequestParam String ids) {
        List<String> requested = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofMinutes(2)).cachePublic())
                .body(productService.findPublishedByIds(requested));
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
